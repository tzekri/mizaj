package ma.mizaj.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import ma.mizaj.domain.JuiceCategory;
import ma.mizaj.domain.JuiceIngredient;
import ma.mizaj.domain.JuiceSize;

/** Composeur de jus : requêtes et réponses JSON. */
public final class JuiceDtos {

	private JuiceDtos() {
	}

	public record IngredientView(String code, String name, JuiceCategory category, int price, String color, boolean available) {

		public static IngredientView of(JuiceIngredient i) {
			return new IngredientView(i.getCode(), i.getName(), i.getCategory(), i.getPrice(), i.getColor(), i.isAvailable());
		}
	}

	public record SizeView(JuiceSize code, String label, int basePrice, double factor) {
	}

	/** Catalogue public : ingrédients disponibles, formats et règles de composition. */
	public record Catalog(List<IngredientView> ingredients, List<SizeView> sizes, int maxFruits, int maxExtras) {
	}

	public record IngredientRequest(
			@NotBlank @Size(max = 40) String name,
			@NotNull JuiceCategory category,
			@Min(0) @Max(200) int price,
			@NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}") String color,
			boolean available) {
	}

	/** ingredients : codes, dans n'importe quel ordre (une base, deux fruits et deux extras au plus). */
	public record RecipeRequest(@Size(max = 40) String name, @NotNull JuiceSize size, @NotEmpty @Size(max = 5) List<String> ingredients) {
	}

	/** available : false si un ingrédient du favori n'est plus proposé (rupture, hors saison). */
	public record RecipeView(String name, JuiceSize size, String sizeLabel, List<IngredientView> ingredients, String label,
			int price, boolean available) {
	}

	/** status : PENDING (à servir), SERVED, EXPIRED (code non utilisé dans les délais). qr : contenu du QR code. */
	public record JuiceOrderView(String code, String qr, RecipeView recipe, Instant createdAt, Instant expiresAt,
			String status, Instant servedAt, String shop) {
	}

	public record FavoriteView(long id, RecipeView recipe, Instant createdAt) {
	}

	public record JuiceScanRequest(@NotBlank @Size(max = 40) String code) {
	}

	public record TopMix(String label, String sizeLabel, long count) {
	}

	/** Statistiques sur une période : jus servis, montant encaissé, codes créés, mix les plus servis. */
	public record JuiceStats(long served, long revenue, long created, List<TopMix> topMixes) {
	}
}
