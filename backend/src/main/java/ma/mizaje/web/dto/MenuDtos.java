package ma.mizaje.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import ma.mizaje.domain.MenuCategory;
import ma.mizaje.domain.MenuItem;
import ma.mizaje.domain.MenuTone;

/** Menu du coffee shop : requêtes et réponses JSON. */
public final class MenuDtos {

	/** Photo : identifiant Unsplash (ex. 1558220829-4694a46bb01f) ou adresse https. */
	static final String PHOTO = "^(https://\\S+|[0-9]{9,14}-[0-9a-f]{6,16})$";
	static final String PHOTO_MESSAGE = "doit être un identifiant de photo Unsplash ou une adresse https";

	private MenuDtos() {
	}

	public record MenuItemView(long id, long categoryId, String name, String description, int price, Integer intensity,
			String volume, String photo, boolean featured, String tag, boolean available) {

		public static MenuItemView of(MenuItem i) {
			return new MenuItemView(i.getId(), i.getCategory().getId(), i.getName(), i.getDescription(), i.getPrice(),
					i.getIntensity(), i.getVolume(), i.getPhoto(), i.isFeatured(), i.getTag(), i.isAvailable());
		}
	}

	public record MenuCategoryView(long id, String slug, String light, String bold, String arabic, MenuTone tone, String photo,
			boolean visible, List<MenuItemView> items) {

		public static MenuCategoryView of(MenuCategory c, List<MenuItemView> items) {
			return new MenuCategoryView(c.getId(), c.getSlug(), c.getLight(), c.getBold(), c.getArabic(), c.getTone(), c.getPhoto(),
					c.isVisible(), items);
		}
	}

	public record CategoryRequest(
			@NotBlank @Size(max = 40) String light,
			@NotBlank @Size(max = 40) String bold,
			@Size(max = 60) String arabic,
			@NotNull MenuTone tone,
			@NotBlank @Size(max = 300) @Pattern(regexp = PHOTO, message = PHOTO_MESSAGE) String photo,
			boolean visible) {
	}

	public record ItemRequest(
			@NotNull Long categoryId,
			@NotBlank @Size(max = 60) String name,
			@Size(max = 200) String description,
			@Min(0) @Max(5000) int price,
			@Min(1) @Max(5) Integer intensity,
			@Size(max = 30) String volume,
			@NotBlank @Size(max = 300) @Pattern(regexp = PHOTO, message = PHOTO_MESSAGE) String photo,
			boolean featured,
			@Size(max = 30) String tag,
			boolean available) {
	}

	/** direction : « up » (remonter) ou « down » (descendre). */
	public record MoveRequest(@NotBlank @Pattern(regexp = "up|down") String direction) {
	}
}
