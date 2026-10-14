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
 * Choix du prestataire d'envoi des codes, par ordre de préférence :
 * 1. Twilio Verify (TWILIO_VERIFY_SID) : solution professionnelle, Twilio gère le code et l'anti-fraude ;
 * 2. Twilio Messages WhatsApp (TWILIO_WHATSAPP_FROM) : code généré par Mizaj, modèle WhatsApp à fournir ;
 * 3. rien : mode démo, le code est affiché à l'écran (OTP_DEV_MODE=true).
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
			@Value("${app.twilio.content-sid}") String contentSid,
			@Value("${app.otp.dev-mode}") boolean devMode) {
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
			log.info("Codes de validation envoyés par Twilio Verify ({}), canaux : {}", verifySid, channels);
			if (devMode) {
				log.warn("OTP_DEV_MODE=true est ignoré avec Twilio Verify : le code n'est connu que de Twilio.");
			}
			return new OtpProviders(new TwilioVerifyClient(verifyUrl, sid, token, verifySid, channels), new LoggingOtpSender());
		}

		if (credentials && !from.isEmpty()) {
			log.info("Codes de validation envoyés par WhatsApp via Twilio Messages depuis {}{}", from,
					contentSid.isEmpty() ? " (texte libre : sandbox uniquement)" : " (modèle " + contentSid + ")");
			return new OtpProviders(null, new TwilioWhatsAppSender(sid, token, from, contentSid));
		}

		if (devMode) {
			log.info("Twilio non configuré : mode démo, les codes sont affichés à l'écran.");
		} else {
			log.warn("Twilio n'est pas configuré et OTP_DEV_MODE=false : aucun client ne pourra recevoir de code !");
		}
		return new OtpProviders(null, new LoggingOtpSender());
	}

	/** Vide si non renseigné ; « - » est accepté comme valeur de remplissage (formulaires exigeant un champ non vide). */
	private static String clean(String s) {
		return s == null || s.isBlank() || s.trim().equals("-") ? "" : s.trim();
	}
}
