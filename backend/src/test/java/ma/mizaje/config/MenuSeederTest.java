package ma.mizaje.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import ma.mizaje.domain.MenuCategory;
import ma.mizaje.domain.MenuItem;
import ma.mizaje.domain.MenuTone;
import ma.mizaje.repo.AppSettingRepository;
import ma.mizaje.repo.MenuCategoryRepository;
import ma.mizaje.repo.MenuItemRepository;
import org.junit.jupiter.api.Test;

class MenuSeederTest {

	@Test
	void rebrandReplacesFormerBrandNames() {
		assertEquals("Kidima Signature", MenuSeeder.rebrand("Mizaje Signature"));
		assertEquals("Purple Kidima", MenuSeeder.rebrand("Purple Mizaj"));
		assertEquals("Kidima Intense", MenuSeeder.rebrand("Marokoffy Intense"));
		assertEquals("Café turc", MenuSeeder.rebrand("Café turc"));
		assertNull(MenuSeeder.rebrand(null));
	}

	@Test
	void newPricesAreAppliedOnceAndKeepAdminChanges() throws Exception {
		MenuCategoryRepository categories = mock(MenuCategoryRepository.class);
		MenuItemRepository items = mock(MenuItemRepository.class);
		AppSettingRepository settings = mock(AppSettingRepository.class);
		MenuCategory coffee = new MenuCategory("coffee", 0);
		coffee.update("Espresso", "Coffee", null, MenuTone.CREAM, "1558220829-4694a46bb01f", true);
		MenuItem espresso = item(coffee, "Espresso", MenuSeeder.OLD_ESPRESSO, 10);
		MenuItem latte = item(coffee, "Café Latte", null, 21); // prix déjà changé par l'administrateur
		when(categories.count()).thenReturn(1L);
		when(items.findAll()).thenReturn(List.of(espresso, latte));
		MenuSeeder seeder = new MenuSeeder(categories, items, new ObjectMapper(), settings);

		seeder.run(null);
		assertEquals(8, espresso.getPrice());
		assertEquals(MenuSeeder.NEW_ESPRESSO, espresso.getDescription());
		assertEquals(21, latte.getPrice());

		// Déjà appliqué : l'espresso remis à 10 DH dans l'administration n'est plus touché
		when(settings.existsById(MenuSeeder.PRICES_2026_10)).thenReturn(true);
		espresso.update("Espresso", MenuSeeder.NEW_ESPRESSO, 10, null, null, espresso.getPhoto(), false, null, true);
		seeder.run(null);
		assertEquals(10, espresso.getPrice());
	}

	private static MenuItem item(MenuCategory c, String name, String description, int price) {
		MenuItem i = new MenuItem(c, 0);
		i.update(name, description, price, null, null, "1558220829-4694a46bb01f", false, null, true);
		return i;
	}
}
