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
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Une période d'abonnement payée au comptoir : N espressos valables D jours à partir de l'activation.
 * Les boissons non consommées ne sont pas reportées sur la période suivante.
 */
@Entity
@Table(name = "subscriptions", indexes = @Index(columnList = "customer_id, activatedAt"))
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Customer customer;

	@Column(nullable = false)
	private Instant activatedAt;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private int drinksTotal;

	@Column(nullable = false)
	private int drinksLeft;

	/** Montant encaissé, en dirhams. */
	@Column(nullable = false)
	private int pricePaid;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private StaffUser activatedBy;

	@Column(length = 60)
	private String shop;

	/** Protège le décompte quand deux baristas scannent la même carte en même temps. */
	@Version
	private long version;

	protected Subscription() {
	}

	public Subscription(Customer customer, Instant activatedAt, Instant expiresAt, int drinks, int pricePaid,
			StaffUser activatedBy) {
		this.customer = customer;
		this.activatedAt = activatedAt;
		this.expiresAt = expiresAt;
		this.drinksTotal = drinks;
		this.drinksLeft = drinks;
		this.pricePaid = pricePaid;
		this.activatedBy = activatedBy;
		this.shop = activatedBy.getShop();
	}

	public SubscriptionStatus statusAt(Instant now) {
		if (!expiresAt.isAfter(now)) {
			return SubscriptionStatus.EXPIRED;
		}
		return drinksLeft > 0 ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EMPTY;
	}

	public void consumeOne() {
		drinksLeft--;
	}

	public void giveBackOne() {
		drinksLeft = Math.min(drinksTotal, drinksLeft + 1);
	}

	/** Remplacée par une nouvelle période avant terme : les boissons restantes sont perdues. */
	public void closeAt(Instant now) {
		if (expiresAt.isAfter(now)) {
			expiresAt = now;
		}
	}

	public Long getId() { return id; }
	public Customer getCustomer() { return customer; }
	public Instant getActivatedAt() { return activatedAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public int getDrinksTotal() { return drinksTotal; }
	public int getDrinksLeft() { return drinksLeft; }
	public int getPricePaid() { return pricePaid; }
	public StaffUser getActivatedBy() { return activatedBy; }
	public String getShop() { return shop; }
}
