package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Produit du menu (boisson, café en grain, viennoiserie), géré par l'administrateur. */
@Entity
@Table(name = "menu_items")
public class MenuItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private MenuCategory category;

	@Column(nullable = false, length = 60)
	private String name;

	@Column(length = 200)
	private String description;

	/** Prix en dirhams. */
	@Column(nullable = false)
	private int price;

	/** Intensité de 1 à 5 (cafés) ; null : non affichée. */
	private Integer intensity;

	/** Contenance affichée (ex. « 250 ml », « Théière ») ; null : non affichée. */
	@Column(length = 30)
	private String volume;

	/** Identifiant de photo Unsplash ou adresse https. */
	@Column(nullable = false, length = 300)
	private String photo;

	/** Mis en avant dans « Produits populaires » sur l'accueil. */
	@Column(nullable = false)
	private boolean featured;

	/** Pastille sur la photo (ex. « Best-seller », « Nouveau ») ; null : aucune. */
	@Column(length = 30)
	private String tag;

	/** false : en rupture, masqué sur le site. */
	@Column(nullable = false)
	private boolean available = true;

	@Column(nullable = false)
	private int sortOrder;

	protected MenuItem() {
	}

	public MenuItem(MenuCategory category, int sortOrder) {
		this.category = category;
		this.sortOrder = sortOrder;
	}

	public void update(String name, String description, int price, Integer intensity, String volume, String photo,
			boolean featured, String tag, boolean available) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.intensity = intensity;
		this.volume = volume;
		this.photo = photo;
		this.featured = featured;
		this.tag = tag;
		this.available = available;
	}

	public Long getId() { return id; }
	public MenuCategory getCategory() { return category; }
	public void setCategory(MenuCategory category) { this.category = category; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public int getPrice() { return price; }
	public Integer getIntensity() { return intensity; }
	public String getVolume() { return volume; }
	public String getPhoto() { return photo; }
	public boolean isFeatured() { return featured; }
	public String getTag() { return tag; }
	public boolean isAvailable() { return available; }
	public int getSortOrder() { return sortOrder; }
	public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
