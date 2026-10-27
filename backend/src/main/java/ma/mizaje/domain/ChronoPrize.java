package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Bon cadeau gagné au défi « 10 secondes » : à montrer en caisse, valable une fois. */
@Entity
@Table(name = "chrono_prizes")
public class ChronoPrize {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Code court lisible (7 caractères), contenu du QR « KIDIMA-GAIN:CODE ». */
	@Column(nullable = false, unique = true, length = 7)
	private String code;

	/** Lot gagné, tel qu'il était proposé au moment du gain (ex. « Un café offert »). */
	@Column(nullable = false, length = 80)
	private String label;

	@Column(nullable = false)
	private int elapsedMs;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant redeemedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	private StaffUser redeemedBy;

	@Column(length = 60)
	private String shop;

	protected ChronoPrize() {
	}

	public ChronoPrize(String code, String label, int elapsedMs, Instant createdAt, Instant expiresAt) {
		this.code = code;
		this.label = label;
		this.elapsedMs = elapsedMs;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public void redeem(StaffUser staff, Instant at) {
		this.redeemedBy = staff;
		this.shop = staff.getShop();
		this.redeemedAt = at;
	}

	public Long getId() { return id; }
	public String getCode() { return code; }
	public String getLabel() { return label; }
	public int getElapsedMs() { return elapsedMs; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getRedeemedAt() { return redeemedAt; }
	public String getShop() { return shop; }
}
