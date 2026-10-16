package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Jus composé par un client, avec un code à présenter en caisse. Le barista le scanne, encaisse et le marque servi.
 * La recette est figée à la création (codes + libellés) : renommer un ingrédient ne modifie pas une commande passée.
 */
@Entity
@Table(name = "juice_orders", indexes = {@Index(columnList = "createdAt"), @Index(columnList = "servedAt")})
public class JuiceOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Code court lisible (ex. K7M2QF), contenu du QR « MIZAJE-JUS:K7M2QF ». */
	@Column(nullable = false, unique = true, length = 8)
	private String code;

	@Column(length = 40)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 4)
	private JuiceSize size;

	/** Codes des ingrédients, base en premier, séparés par « | ». */
	@Column(nullable = false, length = 200)
	private String ingredients;

	/** Libellés au moment de la commande, ex. « Orange · Carotte · Gingembre ». */
	@Column(nullable = false, length = 250)
	private String recipeLabel;

	/** Recette normalisée (codes triés + format) pour regrouper les mix identiques dans les statistiques. */
	@Column(nullable = false, length = 220)
	private String signature;

	@Column(nullable = false)
	private int price;

	@ManyToOne(fetch = FetchType.LAZY)
	private Customer customer;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant servedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	private StaffUser servedBy;

	@Column(length = 60)
	private String shop;

	protected JuiceOrder() {
	}

	public JuiceOrder(String code, String name, JuiceSize size, String ingredients, String recipeLabel, String signature,
			int price, Customer customer, Instant createdAt) {
		this.code = code;
		this.name = name;
		this.size = size;
		this.ingredients = ingredients;
		this.recipeLabel = recipeLabel;
		this.signature = signature;
		this.price = price;
		this.customer = customer;
		this.createdAt = createdAt;
	}

	public void serve(StaffUser staff, Instant now) {
		this.servedAt = now;
		this.servedBy = staff;
		this.shop = staff.getShop();
	}

	public Long getId() { return id; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public JuiceSize getSize() { return size; }
	public String getIngredients() { return ingredients; }
	public String getRecipeLabel() { return recipeLabel; }
	public String getSignature() { return signature; }
	public int getPrice() { return price; }
	public Customer getCustomer() { return customer; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getServedAt() { return servedAt; }
	public StaffUser getServedBy() { return servedBy; }
	public String getShop() { return shop; }
}
