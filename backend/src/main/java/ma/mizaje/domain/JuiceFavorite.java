package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Jus favori d'un abonné, recommandable en un geste depuis sa carte. */
@Entity
@Table(name = "juice_favorites")
public class JuiceFavorite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Customer customer;

	@Column(nullable = false, length = 40)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 4)
	private JuiceSize size;

	/** Codes des ingrédients, base en premier, séparés par « | ». */
	@Column(nullable = false, length = 200)
	private String ingredients;

	@Column(nullable = false)
	private Instant createdAt;

	protected JuiceFavorite() {
	}

	public JuiceFavorite(Customer customer, String name, JuiceSize size, String ingredients, Instant createdAt) {
		this.customer = customer;
		this.name = name;
		this.size = size;
		this.ingredients = ingredients;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public Customer getCustomer() { return customer; }
	public String getName() { return name; }
	public JuiceSize getSize() { return size; }
	public String getIngredients() { return ingredients; }
	public Instant getCreatedAt() { return createdAt; }
}
