package ma.mizaj.otp;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Validation du numéro à l'inscription, selon la configuration Twilio :
 * 1. Twilio Verify (TWILIO_VERIFY_SID) : code envoyé par WhatsApp ou SMS, vérifié par Twilio ;
 * 2. Twilio Messages WhatsApp (TWILIO_WHATSAPP_FROM) : code généré par Mizaje, modèle WhatsApp à fournir ;
 * 3. rien : inscription directe sans code ; une carte existante ne se récupère qu'avec un code donné en caisse.
 */
@Configuration
public class OtpConfig {

	private static final Logger log = LoggerFactory.getLogger(OtpConfig.class);

	@Bean
	OtpProviders otpProviders(
			@Value("${app.twilio.account-sid}") String sid,
			@Value("${app.twilio.auth-token}") String token,
			@Value("${app.twilio.verify-sid}") String verifySid,
			@Value("${app.twilio.verify-channels}") String verifyChannels,
			@Value("${app.twilio.verify-url}") String verifyUrl,
			@Value("${app.twilio.from}") String from,
			@Value("${app.twilio.content-sid}") String contentSid) {
		sid = clean(sid);
		token = clean(token);
		verifySid = clean(verifySid);
		from = clean(from);
		contentSid = clean(contentSid);
		boolean credentials = !sid.isEmpty() && !token.isEmpty();

		if (credentials && !verifySid.isEmpty()) {
			List<OtpChannel> channels = Arrays.stream(verifyChannels.split(","))
					.map(OtpChannel::parse).filter(Objects::nonNull).distinct().toList();
			if (channels.isEmpty()) {
				channels = List.of(OtpChannel.WHATSAPP, OtpChannel.SMS);
			}
			log.info("Inscription avec code : Twilio Verify ({}), canaux {}", verifySid, channels);
			return new OtpProviders(new TwilioVerifyClient(verifyUrl, sid, token, verifySid, channels), null);
		}

		if (credentials && !from.isEmpty()) {
			log.info("Inscription avec code : WhatsApp via Twilio Messages depuis {}{}", from,
					contentSid.isEmpty() ? " (texte libre : sandbox uniquement)" : " (modèle " + contentSid + ")");
			return new OtpProviders(null, new TwilioWhatsAppSender(sid, token, from, contentSid));
		}

		log.info("Twilio non configuré : inscription directe sans code, récupération de carte par code donné en caisse.");
		return new OtpProviders(null, null);
	}

	/** Vide si non renseigné ; « - » est accepté comme valeur de remplissage (formulaires exigeant un champ non vide). */
	private static String clean(String s) {
		return s == null || s.isBlank() || s.trim().equals("-") ? "" : s.trim();
	}
}
