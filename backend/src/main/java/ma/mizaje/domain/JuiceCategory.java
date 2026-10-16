package ma.mizaje.domain;

/** Rôle d'un ingrédient dans un jus composé. */
public enum JuiceCategory {
	/** Exactement une base par jus (orange, pomme, lait pour les smoothies…). */
	BASE,
	/** Jusqu'à deux fruits ajoutés à la base. */
	FRUIT,
	/** Jusqu'à deux extras (menthe, gingembre, miel…). */
	EXTRA
}
