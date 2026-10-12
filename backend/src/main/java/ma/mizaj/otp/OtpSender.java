package ma.mizaj.otp;

/** Envoie le code de validation au client. */
public interface OtpSender {

	/**
	 * @param phone numéro au format E.164 (+212…)
	 * @param code  code à 6 chiffres
	 * @throws OtpDeliveryException si le message n'a pas pu être remis au prestataire
	 */
	void send(String phone, String code);

	/** false : aucun prestataire configuré, le code n'est pas réellement envoyé. */
	boolean delivers();
}
