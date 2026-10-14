package ma.mizaj.otp;

/** Le prestataire n'a pas pu envoyer (ou vérifier) le code. */
public class OtpDeliveryException extends RuntimeException {

	private final Integer twilioCode;
	private final Integer httpStatus;

	public OtpDeliveryException(String message, Throwable cause) {
		this(message, cause, null, null);
	}

	public OtpDeliveryException(String message, Throwable cause, Integer twilioCode, Integer httpStatus) {
		super(message, cause);
		this.twilioCode = twilioCode;
		this.httpStatus = httpStatus;
	}

	/** Code d'erreur Twilio (ex. 60200 numéro invalide, 60203 trop d'envois), s'il est connu. */
	public Integer twilioCode() {
		return twilioCode;
	}

	public Integer httpStatus() {
		return httpStatus;
	}
}
