package ma.mizaje.otp;

/** Canal d'envoi du code de validation. */
public enum OtpChannel {
	WHATSAPP("whatsapp"),
	SMS("sms");

	/** Valeur attendue par l'API Twilio Verify (paramètre Channel). */
	final String twilio;

	OtpChannel(String twilio) {
		this.twilio = twilio;
	}

	public static OtpChannel parse(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		for (OtpChannel c : values()) {
			if (c.twilio.equalsIgnoreCase(value.trim()) || c.name().equalsIgnoreCase(value.trim())) {
				return c;
			}
		}
		return null;
	}
}
