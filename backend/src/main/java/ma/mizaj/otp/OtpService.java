package ma.mizaj.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import ma.mizaj.domain.OtpChallenge;
import ma.mizaj.repo.CustomerRepository;
import ma.mizaj.repo.OtpChallengeRepository;
import ma.mizaj.service.PhoneNumbers;
import ma.mizaj.service.PlanService;
import ma.mizaj.web.ApiException;
import ma.mizaj.web.dto.Dtos.OtpStartResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Codes à 6 chiffres : validation du numéro à l'inscription (si Twilio est configuré) et codes de récupération
 * donnés en caisse par le barista.
 * <p>
 * Avec Twilio Verify, Twilio génère et vérifie le code ; sinon Mizaje le génère et n'en garde que l'empreinte.
 * Garde-fous : 10 min de validité, 5 essais par code, 60 s entre deux envois,
 * 5 envois par heure et par numéro, 15 par heure et par adresse IP.
 */
@Service
public class OtpService {

	private static final Logger log = LoggerFactory.getLogger(OtpService.class);
	static final Duration VALIDITY = Duration.ofMinutes(10);
	static final Duration RESEND_DELAY = Duration.ofSeconds(60);
	static final int MAX_SENDS_PER_HOUR = 5;
	static final int MAX_ATTEMPTS = 5;

	private final OtpChallengeRepository challenges;
	private final CustomerRepository customers;
	private final OtpProviders providers;
	private final PlanService plans;
	private final IpThrottle ipThrottle;
	private final SecretKey key;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public OtpService(OtpChallengeRepository challenges, CustomerRepository customers, OtpProviders providers,
			PlanService plans, IpThrottle ipThrottle, SecretKey jwtKey, Clock clock) {
		this.challenges = challenges;
		this.customers = customers;
		this.providers = providers;
		this.plans = plans;
		this.ipThrottle = ipThrottle;
		this.key = jwtKey;
		this.clock = clock;
	}

	/** true : un code est envoyé au client à l'inscription (Twilio configuré) ; false : inscription directe. */
	public boolean verificationEnabled() {
		return providers.enabled();
	}

	@Transactional
	public OtpStartResponse start(String rawPhone, String firstName, String planCode, String rawChannel, String ip) {
		if (!providers.enabled()) {
			throw ApiException.notFound("L'envoi de code n'est pas activé : inscrivez-vous directement avec votre numéro.");
		}
		String phone = PhoneNumbers.normalize(rawPhone);
		var plan = plans.activeByCode(planCode);
		OtpChannel requested = OtpChannel.parse(rawChannel);
		Instant now = Instant.now(clock);
		challenges.findFirstByPhoneOrderByCreatedAtDesc(phone).ifPresent(last -> {
			long wait = Duration.between(now, last.getCreatedAt().plus(RESEND_DELAY)).toSeconds();
			if (wait > 0) {
				throw ApiException.tooMany("Un code vient d'être envoyé. Patientez " + wait + " s avant d'en demander un autre.");
			}
		});
		if (challenges.countByPhoneAndCreatedAtAfter(phone, now.minus(Duration.ofHours(1))) >= MAX_SENDS_PER_HOUR) {
			throw ApiException.tooMany("Trop de codes demandés pour ce numéro. Réessayez dans une heure.");
		}
		if (!ipThrottle.tryAcquire(ip)) {
			throw ApiException.tooMany("Trop de demandes depuis cet appareil. Réessayez plus tard.");
		}

		String name = firstName == null || firstName.isBlank() ? null : firstName.trim();
		String planKey = plan == null ? null : plan.getCode();
		boolean newAccount = customers.findByPhone(phone).isEmpty();

		if (providers.managed()) {
			OtpChannel used;
			try {
				used = providers.verify().start(phone, requested);
			} catch (OtpDeliveryException e) {
				throw deliveryError(phone, e);
			}
			OtpChallenge challenge = new OtpChallenge(phone, "", name, planKey, now, now.plus(VALIDITY));
			challenge.deliveredBy(used.twilio, true);
			challenges.save(challenge);
			return new OtpStartResponse(PhoneNumbers.pretty(phone), newAccount, (int) VALIDITY.toSeconds(),
					(int) RESEND_DELAY.toSeconds(), used.twilio, providers.verify().channels().contains(OtpChannel.SMS));
		}

		// Twilio Messages : code généré ici, seule son empreinte est stockée.
		String code = newCode();
		OtpChallenge challenge = new OtpChallenge(phone, hash(phone, code), name, planKey, now, now.plus(VALIDITY));
		challenge.deliveredBy(OtpChannel.WHATSAPP.twilio, false);
		challenges.save(challenge);
		try {
			providers.messages().send(phone, code);
		} catch (OtpDeliveryException e) {
			throw deliveryError(phone, e);
		}
		return new OtpStartResponse(PhoneNumbers.pretty(phone), newAccount, (int) VALIDITY.toSeconds(),
				(int) RESEND_DELAY.toSeconds(), OtpChannel.WHATSAPP.twilio, false);
	}

	/**
	 * Code de récupération donné de vive voix par le barista au client présent en caisse
	 * (nouveau téléphone, carte perdue). Remplace tout code en cours pour ce numéro.
	 */
	@Transactional
	public RecoveryCode issueRecoveryCode(String phone) {
		Instant now = Instant.now(clock);
		String code = newCode();
		OtpChallenge challenge = new OtpChallenge(phone, hash(phone, code), null, null, now, now.plus(VALIDITY));
		challenge.deliveredBy("counter", false);
		challenges.save(challenge);
		return new RecoveryCode(code, challenge.getExpiresAt());
	}

	public record RecoveryCode(String code, Instant expiresAt) {
	}

	private String newCode() {
		return "%06d".formatted(random.nextInt(1_000_000));
	}

	/**
	 * Vérifie le code et le consomme.
	 * Les essais ratés sont enregistrés même si une erreur est renvoyée (pas de rollback sur ApiException).
	 *
	 * @return le défi validé (numéro normalisé + prénom et formule saisis à l'inscription)
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public OtpChallenge verify(String rawPhone, String code) {
		String phone = PhoneNumbers.normalize(rawPhone);
		Instant now = Instant.now(clock);
		OtpChallenge challenge = challenges.findFirstByPhoneOrderByCreatedAtDesc(phone)
				.filter(c -> c.getUsedAt() == null && c.getExpiresAt().isAfter(now))
				.orElseThrow(() -> ApiException.badRequest("Ce code a expiré. Demandez-en un nouveau."));
		if (challenge.getAttempts() >= MAX_ATTEMPTS) {
			throw ApiException.tooMany("Trop d'essais. Demandez un nouveau code.");
		}
		challenge.recordAttempt();

		boolean valid;
		if (challenge.isManaged()) {
			if (!providers.managed()) {
				throw ApiException.badRequest("Ce code a expiré. Demandez-en un nouveau.");
			}
			TwilioVerifyClient.CheckResult result;
			try {
				result = providers.verify().check(phone, code);
			} catch (OtpDeliveryException e) {
				log.warn("Vérification Twilio impossible pour {} : {}", phone, e.getMessage());
				throw new ApiException(HttpStatus.BAD_GATEWAY, "Vérification impossible pour le moment. Réessayez dans un instant.");
			}
			if (result == TwilioVerifyClient.CheckResult.EXPIRED) {
				throw ApiException.badRequest("Ce code a expiré. Demandez-en un nouveau.");
			}
			valid = result == TwilioVerifyClient.CheckResult.APPROVED;
		} else {
			valid = MessageDigest.isEqual(hash(phone, code).getBytes(StandardCharsets.US_ASCII),
					challenge.getCodeHash().getBytes(StandardCharsets.US_ASCII));
		}

		if (!valid) {
			int left = MAX_ATTEMPTS - challenge.getAttempts();
			throw ApiException.badRequest(left > 0
					? "Code incorrect. Il vous reste " + left + (left > 1 ? " essais." : " essai.")
					: "Code incorrect. Demandez un nouveau code.");
		}
		challenge.markUsed(now);
		return challenge;
	}

	private ApiException deliveryError(String phone, OtpDeliveryException e) {
		log.warn("Envoi du code à {} impossible : {}", phone, e.getCause() == null ? e.getMessage() : e.getMessage() + " / " + e.getCause().getMessage());
		Integer code = e.twilioCode();
		if (code != null && code == 60200) {
			return ApiException.badRequest("Ce numéro n'est pas valide. Vérifiez-le et réessayez.");
		}
		if (code != null && (code == 60203 || code == 60212)) {
			return ApiException.tooMany("Trop de codes demandés pour ce numéro. Réessayez plus tard.");
		}
		if (code != null && code == 60410) {
			return new ApiException(HttpStatus.FORBIDDEN, "Ce numéro ne peut pas recevoir de code pour le moment.");
		}
		return new ApiException(HttpStatus.BAD_GATEWAY,
				"Impossible d'envoyer le code. Vérifiez le numéro (WhatsApp ou SMS), puis réessayez.");
	}

	private String hash(String phone, String code) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(key);
			return HexFormat.of().formatHex(mac.doFinal((phone + ":" + code).getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** Les défis de plus de 24 h ne servent plus (ni au contrôle des renvois, ni à la validation). */
	@Scheduled(cron = "0 17 3 * * *")
	void purge() {
		challenges.deleteOlderThan(Instant.now(clock).minus(Duration.ofDays(1)));
	}
}
