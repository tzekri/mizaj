package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/** Nombre d'affichages d'une pub partenaire, par jour et par coffee shop (chiffres à montrer au partenaire). */
@Entity
@Table(name = "partner_displays", uniqueConstraints = @UniqueConstraint(columnNames = {"partner_id", "display_day", "shop"}))
public class PartnerDisplay {

	/** Coffee shop d'un écran sans boutique précisée. */
	public static final String NO_SHOP = "-";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Partner partner;

	/** « day » est un mot réservé de certaines bases (H2). */
	@Column(name = "display_day", nullable = false)
	private LocalDate day;

	@Column(nullable = false, length = 60)
	private String shop;

	@Column(nullable = false)
	private long shown;

	protected PartnerDisplay() {
	}

	public PartnerDisplay(Partner partner, LocalDate day, String shop) {
		this.partner = partner;
		this.day = day;
		this.shop = shop;
	}

	public void increment() {
		shown++;
	}

	public long getShown() { return shown; }
}
