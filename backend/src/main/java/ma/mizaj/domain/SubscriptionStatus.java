package ma.mizaj.domain;

/** État de la carte d'un client. */
public enum SubscriptionStatus {
	/** Compte créé, jamais activé : le client doit payer en caisse. */
	NONE,
	ACTIVE,
	/** Toutes les boissons de la période ont été consommées. */
	EMPTY,
	/** Période de validité dépassée. */
	EXPIRED
}
