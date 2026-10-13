package ma.mizaj.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Client abonné, identifié par son numéro de téléphone (validé par code WhatsApp). */
@Entity
@Table(name = "customers")
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Format E.164, ex. +212612345678. */
	@Column(nullable = false, unique = true, length = 20)
	private String phone;

	@Column(length = 60)
	private String firstName;

	/** Contenu du QR code de la carte : aléatoire, sans lien avec le numéro. Peut être régénéré. */
	@Column(nullable = false, unique = true, length = 40)
	private String qrToken;

	@Column(nullable = false)
	private Instant createdAt;

	/** Formule choisie par le client (proposée par défaut au barista lors de l'activation). */
	@ManyToOne(fetch = FetchType.LAZY)
	private Plan preferredPlan;

	protected Customer() {
	}

	public Customer(String phone, String firstName, String qrToken, Instant createdAt) {
		this.phone = phone;
		this.firstName = firstName;
		this.qrToken = qrToken;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public String getPhone() { return phone; }
	public String getFirstName() { return firstName; }
	public void setFirstName(String firstName) { this.firstName = firstName; }
	public String getQrToken() { return qrToken; }
	public void setQrToken(String qrToken) { this.qrToken = qrToken; }
	public Instant getCreatedAt() { return createdAt; }
	public Plan getPreferredPlan() { return preferredPlan; }
	public void setPreferredPlan(Plan preferredPlan) { this.preferredPlan = preferredPlan; }
}
