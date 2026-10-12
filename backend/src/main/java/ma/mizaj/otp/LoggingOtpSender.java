package ma.mizaj.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilisé tant que Twilio n'est pas configuré : le code est seulement écrit dans les logs. */
public class LoggingOtpSender implements OtpSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

	@Override
	public void send(String phone, String code) {
		log.info("[OTP non envoyé — Twilio non configuré] {} → {}", phone, code);
	}

	@Override
	public boolean delivers() {
		return false;
	}
}
