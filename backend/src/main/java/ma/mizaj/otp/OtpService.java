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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Codes de validation à 6 chiffres envoyés par WhatsApp.
 * Garde-fous : 10 min de validité, 5 essais par code, 60 s entre deux envois, 5 envois par heure et par numéro.
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
	private final OtpSender sender;
	private final PlanService plans;
	private final IpThrottle ipThrottle;
	private final SecretKey key;
	private final Clock clock;
	private final boolean devMode;
	private final SecureRandom random = new SecureRandom();

	public OtpService(OtpChallengeRepository challenges, CustomerRepository customers, OtpSender sender,
			PlanService plans, IpThrottle ipThrottle, SecretKey jwtKey, Clock clock, @Value("${app.otp.dev-mode}") boolean devMode) {
		this.challenges = challenges;
		this.customers = customers;
		this.sender = sender;
		this.plans = plans;
		this.ipThrottle = ipThrottle;
		this.key = jwtKey;
		this.clock = clock;
		this.devMode = devMode;
	}

	@Transactional
	public OtpStartResponse start(String rawPhone, String firstName, String planCode, String ip) {
		String phone = PhoneNumbers.normalize(rawPhone);
		var plan = plans.activeByCode(planCode);
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

		String code = "%06d".formatted(random.nextInt(1_000_000));
		String name = firstName == null || firstName.isBlank() ? null : firstName.trim();
		challenges.save(new OtpChallenge(phone, hash(phone, code), name, plan == null ? null : plan.getCode(), now, now.plus(VALIDITY)));
		try {
			sender.send(phone, code);
		} catch (OtpDeliveryException e) {
			log.warn("Envoi du code à {} impossible : {}", phone, e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
			throw new ApiException(HttpStatus.BAD_GATEWAY,
					"Impossible d'envoyer le code sur WhatsApp. Vérifiez que ce numéro utilise WhatsApp, puis réessayez.");
		}
		return new OtpStartResponse(PhoneNumbers.pretty(phone), customers.findByPhone(phone).isEmpty(),
				(int) VALIDITY.toSeconds(), (int) RESEND_DELAY.toSeconds(), sender.delivers(), devMode ? code : null);
	}

	/**
	 * Vérifie le code et le consomme.
	 * Les essais ratés sont enregistrés même si une erreur est renvoyée (pas de rollback sur ApiException).
	 *
	 * @return le défi validé (numéro normalisé + prénom saisi à l'inscription)
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
		if (!MessageDigest.isEqual(hash(phone, code).getBytes(StandardCharsets.US_ASCII),
				challenge.getCodeHash().getBytes(StandardCharsets.US_ASCII))) {
			int left = MAX_ATTEMPTS - challenge.getAttempts();
			throw ApiException.badRequest(left > 0
					? "Code incorrect. Il vous reste " + left + (left > 1 ? " essais." : " essai.")
					: "Code incorrect. Demandez un nouveau code.");
		}
		challenge.markUsed(now);
		return challenge;
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
