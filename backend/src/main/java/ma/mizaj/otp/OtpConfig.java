package ma.mizaj.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpConfig {

	private static final Logger log = LoggerFactory.getLogger(OtpConfig.class);

	@Bean
	OtpSender otpSender(
			@Value("${app.twilio.account-sid}") String sid,
			@Value("${app.twilio.auth-token}") String token,
			@Value("${app.twilio.from}") String from,
			@Value("${app.twilio.content-sid}") String contentSid,
			@Value("${app.otp.dev-mode}") boolean devMode) {
		if (sid.isBlank() || token.isBlank() || from.isBlank()) {
			if (!devMode) {
				log.warn("Twilio n'est pas configuré et OTP_DEV_MODE=false : aucun client ne pourra recevoir de code !");
			} else {
				log.info("Twilio non configuré : mode démo, les codes sont affichés à l'écran.");
			}
			return new LoggingOtpSender();
		}
		log.info("Codes de validation envoyés par WhatsApp via Twilio depuis {}{}", from,
				contentSid.isBlank() ? " (texte libre : sandbox uniquement)" : " (modèle " + contentSid + ")");
		return new TwilioWhatsAppSender(sid, token, from, contentSid);
	}
}
