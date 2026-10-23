package ma.mizaje.otp;

/**
 * Prestataires d'envoi de code configurés (au plus un des deux).
 *
 * @param verify   Twilio Verify (Twilio gère le code) ; null s'il n'est pas configuré
 * @param messages envoi d'un code généré par Kidima via Twilio Messages ; null s'il n'est pas configuré
 */
public record OtpProviders(TwilioVerifyClient verify, OtpSender messages) {

	/** false : aucun envoi possible, l'inscription se fait sans code. */
	public boolean enabled() {
		return verify != null || messages != null;
	}

	public boolean managed() {
		return verify != null;
	}
}
