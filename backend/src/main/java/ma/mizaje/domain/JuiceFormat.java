package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Prix d'un format de jus réglé par l'administrateur ; sans ligne, les valeurs par défaut de {@link JuiceSize} s'appliquent. */
@Entity
@Table(name = "juice_formats")
public class JuiceFormat {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(length = 4)
	private JuiceSize size;

	@Column(nullable = false)
	private int basePrice;

	@Column(nullable = false)
	private double factor;

	protected JuiceFormat() {
	}

	public JuiceFormat(JuiceSize size, int basePrice, double factor) {
		this.size = size;
		update(basePrice, factor);
	}

	public final void update(int basePrice, double factor) {
		this.basePrice = basePrice;
		this.factor = factor;
	}

	public JuiceSize getSize() { return size; }
	public int getBasePrice() { return basePrice; }
	public double getFactor() { return factor; }

	/** Prix : départ du format + suppléments des ingrédients (majorés pour le grand format), arrondi au dirham. */
	public int price(int ingredientsTotal) {
		return basePrice + (int) Math.round(ingredientsTotal * factor);
	}
}
