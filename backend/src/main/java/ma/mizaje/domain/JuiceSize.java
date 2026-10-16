package ma.mizaje.domain;

/** Format du jus : prix de départ en dirhams et coefficient appliqué aux suppléments. */
public enum JuiceSize {
	S33("33 cl", 15, 1.0),
	S50("50 cl", 20, 1.4);

	public final String label;
	public final int basePrice;
	public final double factor;

	JuiceSize(String label, int basePrice, double factor) {
		this.label = label;
		this.basePrice = basePrice;
		this.factor = factor;
	}

	/** Prix : départ du format + suppléments des ingrédients (majorés pour le grand format), arrondi au dirham. */
	public int price(int ingredientsTotal) {
		return basePrice + (int) Math.round(ingredientsTotal * factor);
	}
}
