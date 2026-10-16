package ma.mizaj.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ingrédient proposé dans le composeur de jus (géré par l'administrateur). */
@Entity
@Table(name = "juice_ingredients")
public class JuiceIngredient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 40)
	private String code;

	@Column(nullable = false, length = 40)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private JuiceCategory category;

	/** Supplément en dirhams pour un 33 cl (majoré pour le 50 cl). */
	@Column(nullable = false)
	private int price;

	/** Couleur de l'ingrédient dans l'aperçu du verre (#rrggbb). */
	@Column(nullable = false, length = 7)
	private String color;

	/** false : en rupture ou hors saison, masqué dans le composeur. */
	@Column(nullable = false)
	private boolean available = true;

	@Column(nullable = false)
	private int sortOrder;

	protected JuiceIngredient() {
	}

	public JuiceIngredient(String code, String name, JuiceCategory category, int price, String color, int sortOrder) {
		this.code = code;
		update(name, category, price, color, true);
		this.sortOrder = sortOrder;
	}

	public final void update(String name, JuiceCategory category, int price, String color, boolean available) {
		this.name = name;
		this.category = category;
		this.price = price;
		this.color = color;
		this.available = available;
	}

	public Long getId() { return id; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public JuiceCategory getCategory() { return category; }
	public int getPrice() { return price; }
	public String getColor() { return color; }
	public boolean isAvailable() { return available; }
	public int getSortOrder() { return sortOrder; }
}
