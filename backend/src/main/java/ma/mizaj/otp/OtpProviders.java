package ma.mizaj.otp;

/**
 * Prestataires d'envoi configurés.
 *
 * @param verify Twilio Verify (Twilio gère le code) ; null s'il n'est pas configuré
 * @param local  envoi d'un code généré par Mizaj (Twilio Messages, ou simple log en démo), utilisé sans Verify
 */
public record OtpProviders(TwilioVerifyClient verify, OtpSender local) {

	public boolean managed() {
		return verify != null;
	}
}
