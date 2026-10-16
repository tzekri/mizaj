package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catégorie du menu (ex. « Hot Coffee »), gérée par l'administrateur. */
@Entity
@Table(name = "menu_categories")
public class MenuCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Adresse de la page : /menu/{slug}. */
	@Column(nullable = false, unique = true, length = 60)
	private String slug;

	/** Mot fin du titre (ex. « Hot »). */
	@Column(nullable = false, length = 40)
	private String light;

	/** Mot gras du titre (ex. « Coffee »). */
	@Column(nullable = false, length = 40)
	private String bold;

	@Column(length = 60)
	private String arabic;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MenuTone tone;

	/** Identifiant de photo Unsplash ou adresse https. */
	@Column(nullable = false, length = 300)
	private String photo;

	/** false : catégorie masquée sur le site. */
	@Column(nullable = false)
	private boolean visible = true;

	@Column(nullable = false)
	private int sortOrder;

	protected MenuCategory() {
	}

	public MenuCategory(String slug, int sortOrder) {
		this.slug = slug;
		this.sortOrder = sortOrder;
	}

	public void update(String light, String bold, String arabic, MenuTone tone, String photo, boolean visible) {
		this.light = light;
		this.bold = bold;
		this.arabic = arabic;
		this.tone = tone;
		this.photo = photo;
		this.visible = visible;
	}

	public Long getId() { return id; }
	public String getSlug() { return slug; }
	public String getLight() { return light; }
	public String getBold() { return bold; }
	public String getArabic() { return arabic; }
	public MenuTone getTone() { return tone; }
	public String getPhoto() { return photo; }
	public boolean isVisible() { return visible; }
	public int getSortOrder() { return sortOrder; }
	public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
