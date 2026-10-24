package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Formule d'abonnement proposée aux clients (ex. « Espresso » : 135 DH, 30 espressos, 30 jours).
 * Modifiable par l'administrateur ; une période déjà activée garde son prix et son nombre de boissons.
 */
@Entity
@Table(name = "plans")
public class Plan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Identifiant stable utilisé dans les URL et l'API (ex. « espresso »). */
	@Column(nullable = false, unique = true, length = 40)
	private String code;

	@Column(nullable = false, length = 60)
	private String name;

	@Column(length = 160)
	private String description;

	/** Prix en dirhams. */
	@Column(nullable = false)
	private int price;

	@Column(nullable = false)
	private int drinks;

	@Column(nullable = false)
	private int days;

	/** Boisson décomptée, au singulier et au pluriel (« espresso » / « espressos »). */
	@Column(nullable = false, length = 40)
	private String unit;

	@Column(nullable = false, length = 40)
	private String unitPlural;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private int sortOrder;

	protected Plan() {
	}

	public Plan(String code, String name, String description, int price, int drinks, int days, String unit,
			String unitPlural, int sortOrder) {
		this.code = code;
		update(name, description, price, drinks, days, unit, unitPlural);
		this.sortOrder = sortOrder;
	}

	public final void update(String name, String description, int price, int drinks, int days, String unit,
			String unitPlural) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.drinks = drinks;
		this.days = days;
		this.unit = unit;
		this.unitPlural = unitPlural;
	}

	public Long getId() { return id; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public int getPrice() { return price; }
	public int getDrinks() { return drinks; }
	public int getDays() { return days; }
	public String getUnit() { return unit; }
	public String getUnitPlural() { return unitPlural; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	public int getSortOrder() { return sortOrder; }
	public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
