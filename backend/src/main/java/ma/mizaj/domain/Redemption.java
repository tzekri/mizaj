package ma.mizaj.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Un espresso servi et décompté de l'abonnement (scan validé par le barista). */
@Entity
@Table(name = "redemptions", indexes = {
		@Index(columnList = "subscription_id, redeemedAt"),
		@Index(columnList = "redeemedAt")})
public class Redemption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Subscription subscription;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private StaffUser staff;

	@Column(length = 60)
	private String shop;

	@Column(nullable = false)
	private Instant redeemedAt;

	/** Renseigné si le barista a annulé la validation (erreur de scan). */
	private Instant cancelledAt;

	protected Redemption() {
	}

	public Redemption(Subscription subscription, StaffUser staff, Instant redeemedAt) {
		this.subscription = subscription;
		this.staff = staff;
		this.shop = staff.getShop();
		this.redeemedAt = redeemedAt;
	}

	public void cancel(Instant now) {
		this.cancelledAt = now;
	}

	public Long getId() { return id; }
	public Subscription getSubscription() { return subscription; }
	public StaffUser getStaff() { return staff; }
	public String getShop() { return shop; }
	public Instant getRedeemedAt() { return redeemedAt; }
	public Instant getCancelledAt() { return cancelledAt; }
}
