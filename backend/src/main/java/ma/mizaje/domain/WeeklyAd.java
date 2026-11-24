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
 * Pub de la semaine : une affiche (image en base, voir WeeklyAdPicture) mettant en avant un produit,
 * affichée sur la page /promo pendant sa période.
 */
@Entity
@Table(name = "weekly_ads")
public class WeeklyAd {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Produit mis en avant (ex. « Affogato ») : titre de la page et texte de remplacement de l'image. */
	@Column(nullable = false, length = 60)
	private String title;

	/** Phrase d'accroche facultative, affichée sous l'affiche. */
	@Column(length = 160)
	private String description;

	/** Période de diffusion, incluse (une semaine par défaut). */
	@Column(nullable = false)
	private LocalDate startsOn;

	@Column(nullable = false)
	private LocalDate endsOn;

	@Column(nullable = false)
	private boolean active = true;

	/** Type de l'image (ex. image/jpeg) ; null : pas encore d'image. */
	@Column(length = 40)
	private String imageType;

	/** Change à chaque modification : version de l'adresse de l'image (cache des écrans). */
	@Column(nullable = false)
	private Instant updatedAt;

	protected WeeklyAd() {
	}

	public WeeklyAd(Instant now) {
		this.updatedAt = now;
	}

	public void update(String title, String description, LocalDate startsOn, LocalDate endsOn, boolean active, Instant now) {
		this.title = title;
		this.description = description;
		this.startsOn = startsOn;
		this.endsOn = endsOn;
		this.active = active;
		this.updatedAt = now;
	}

	public void setImageType(String imageType, Instant now) {
		this.imageType = imageType;
		this.updatedAt = now;
	}

	public Long getId() { return id; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public LocalDate getStartsOn() { return startsOn; }
	public LocalDate getEndsOn() { return endsOn; }
	public boolean isActive() { return active; }
	public String getImageType() { return imageType; }
	public Instant getUpdatedAt() { return updatedAt; }
	public boolean hasImage() { return imageType != null; }
}
