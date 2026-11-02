package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Partenaire (commerce du quartier) dont la pub passe sur l'écran géant entre deux parties du défi.
 * L'image est gardée en base, à part (PartnerPicture) : le disque du serveur en ligne est effacé à chaque déploiement.
 */
@Entity
@Table(name = "partners")
public class Partner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 60)
	private String name;

	/** Accroche affichée en grand (ex. « -10 % sur présentation de votre carte Kidima »). */
	@Column(nullable = false, length = 120)
	private String offer;

	/** Ligne secondaire (adresse, conditions) ; null : aucune. */
	@Column(length = 200)
	private String details;

	/** Coffee shops où passe la pub, séparés par des virgules ; null : tous. */
	@Column(length = 200)
	private String shops;

	/** Période de diffusion (incluse) ; null : sans limite. */
	private LocalDate startsOn;
	private LocalDate endsOn;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private int sortOrder;

	/** Type de l'image (ex. image/jpeg) ; null : pas d'image. */
	@Column(length = 40)
	private String imageType;

	/** Change à chaque modification : sert de version à l'adresse de l'image (cache des TV). */
	@Column(nullable = false)
	private Instant updatedAt;

	protected Partner() {
	}

	public Partner(int sortOrder, Instant now) {
		this.sortOrder = sortOrder;
		this.updatedAt = now;
	}

	public void update(String name, String offer, String details, String shops, LocalDate startsOn, LocalDate endsOn, boolean active,
			Instant now) {
		this.name = name;
		this.offer = offer;
		this.details = details;
		this.shops = shops;
		this.startsOn = startsOn;
		this.endsOn = endsOn;
		this.active = active;
		this.updatedAt = now;
	}

	public void setImageType(String imageType, Instant now) {
		this.imageType = imageType;
		this.updatedAt = now;
	}

	public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

	public Long getId() { return id; }
	public String getName() { return name; }
	public String getOffer() { return offer; }
	public String getDetails() { return details; }
	public String getShops() { return shops; }
	public LocalDate getStartsOn() { return startsOn; }
	public LocalDate getEndsOn() { return endsOn; }
	public boolean isActive() { return active; }
	public int getSortOrder() { return sortOrder; }
	public String getImageType() { return imageType; }
	public Instant getUpdatedAt() { return updatedAt; }
	public boolean hasImage() { return imageType != null; }
}
