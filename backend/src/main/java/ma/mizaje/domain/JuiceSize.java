package ma.mizaje.domain;

/**
 * Format du jus, avec son prix de départ et le coefficient appliqué aux suppléments par défaut
 * (modifiables dans l'administration, voir {@link JuiceFormat}).
 */
public enum JuiceSize {
	S33("33 cl", 15, 1.0),
	S50("50 cl", 20, 1.4);

	public final String label;
	public final int defaultBasePrice;
	public final double defaultFactor;

	JuiceSize(String label, int defaultBasePrice, double defaultFactor) {
		this.label = label;
		this.defaultBasePrice = defaultBasePrice;
		this.defaultFactor = defaultFactor;
	}

	public JuiceFormat defaultFormat() {
		return new JuiceFormat(this, defaultBasePrice, defaultFactor);
	}
}
