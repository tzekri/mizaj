package ma.mizaje.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
		when(settings.existsById(MenuSeeder.COLD_BOARD_2026_11)).thenReturn(true);
		when(settings.existsById(MenuSeeder.HOT_PRICES_2026_11)).thenReturn(true);
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

	@Test
	void menuKeepsOnlyTheBoardDrinksWithTheirCurrentPrices() throws Exception {
		MenuCategoryRepository categories = mock(MenuCategoryRepository.class);
		MenuItemRepository items = mock(MenuItemRepository.class);
		AppSettingRepository settings = mock(AppSettingRepository.class);
		when(settings.existsById(MenuSeeder.COLD_BOARD_2026_11)).thenReturn(true);
		when(settings.existsById(MenuSeeder.HOT_PRICES_2026_11)).thenReturn(true);
		MenuCategory coffee = category("coffee", 0);
		MenuCategory hot = category("hot-coffee", 1);
		MenuCategory tea = category("tea", 2);
		MenuCategory pastries = category("viennoiseries", 3);
		MenuItem americano = item(coffee, "Américano", null, 10);
		MenuItem ristretto = item(coffee, "Ristretto", null, 8);
		MenuItem mocaccino = item(hot, "Mocaccino", null, 20);
		MenuItem matcha = item(tea, "Matcha Latte", null, 25);
		MenuItem croissant = item(pastries, "Croissant pur beurre", null, 10);
		when(categories.count()).thenReturn(4L);
		when(categories.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(coffee, hot, tea, pastries));
		when(items.findAll()).thenReturn(List.of(americano, ristretto, mocaccino, matcha, croissant));
		when(settings.existsById(MenuSeeder.PRICES_2026_10)).thenReturn(true);
		List<MenuItem> created = new ArrayList<>();
		when(items.save(any(MenuItem.class))).thenAnswer(i -> {
			created.add(i.getArgument(0));
			return i.getArgument(0);
		});

		new MenuSeeder(categories, items, new ObjectMapper(), settings).run(null);

		// Renommés comme sur le tableau, prix actuels gardés
		assertEquals("Americano", americano.getName());
		assertEquals(10, americano.getPrice());
		assertEquals("Mocha", mocaccino.getName());
		assertTrue(mocaccino.isAvailable());
		// Matcha Latte rejoint les cafés chauds
		assertSame(hot, matcha.getCategory());
		assertTrue(matcha.isAvailable());
		// Hors tableau : masqués, pas supprimés
		assertFalse(ristretto.isAvailable());
		assertFalse(croissant.isAvailable());
		assertFalse(tea.isVisible());
		assertFalse(pastries.isVisible());
		assertTrue(coffee.isVisible() && hot.isVisible());
		// Nouvelles boissons, au prix du tableau ; une boisson existante absente n'est pas recréée à 0 DH
		assertEquals(List.of("Double Espresso", "Chocolat chaud", "Chocolat fondu", "Pistachio Latte", "Ubé Latte"),
				created.stream().map(MenuItem::getName).toList());
		assertEquals(List.of(14, 12, 18, 24, 30), created.stream().map(MenuItem::getPrice).toList());
		assertEquals(mocaccino.getPhoto(), created.get(1).getPhoto());
	}

	private static MenuCategory category(String slug, int order) {
		MenuCategory c = new MenuCategory(slug, order);
		c.update(slug, slug, null, MenuTone.CREAM, "1558220829-4694a46bb01f", true);
		return c;
	}

	private static MenuItem item(MenuCategory c, String name, String description, int price) {
		MenuItem i = new MenuItem(c, 0);
		i.update(name, description, price, null, null, "1558220829-4694a46bb01f", false, null, true);
		return i;
	}

	@Test
	void coldBoardReactivatesRenamesAndAddsColdDrinks() throws Exception {
		MenuCategoryRepository categories = mock(MenuCategoryRepository.class);
		MenuItemRepository items = mock(MenuItemRepository.class);
		AppSettingRepository settings = mock(AppSettingRepository.class);
		MenuCategory coffee = category("coffee", 0);
		MenuCategory ice = category("ice-coffee", 1);
		ice.update("Ice", "Coffee", null, MenuTone.DARK, "1592663527359-cf6642f54cff", false); // masquée par le 1er tableau
		MenuItem americano = item(ice, "Iced Americano", null, 14);
		MenuItem coldBrew = item(ice, "Cold Brew", null, 20);
		MenuItem latte = item(ice, "Iced Latte", "Lait froid.", 18);
		for (MenuItem i : List.of(americano, coldBrew, latte)) {
			i.update(i.getName(), i.getDescription(), i.getPrice(), null, null, i.getPhoto(), false, null, false);
		}
		MenuItem chocolat = item(coffee, "Chocolat chaud", "Chocolat chaud onctueux au lait. Grand (L) : 16 DH.", 12);
		when(categories.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(coffee, ice));
		when(categories.save(any(MenuCategory.class))).thenAnswer(i -> i.getArgument(0));
		when(items.findAll()).thenReturn(List.of(americano, coldBrew, latte, chocolat));
		List<MenuItem> created = new ArrayList<>();
		when(items.save(any(MenuItem.class))).thenAnswer(i -> {
			created.add(i.getArgument(0));
			return i.getArgument(0);
		});

		new MenuSeeder(categories, items, new ObjectMapper(), settings).applyColdBoard();

		assertTrue(ice.isVisible());
		assertEquals("Café", ice.getBold());
		assertEquals("Ice Americano", americano.getName());
		assertTrue(americano.isAvailable());
		assertEquals("Ice Latte Signature", latte.getName());
		assertEquals(25, latte.getPrice()); // produit masqué : prix du tableau
		assertEquals("Vanille & coco.", latte.getDescription());
		assertFalse(coldBrew.isAvailable()); // hors tableau
		// Nouveaux produits (Ice Caramel Latte, Ice Mocha, Ice Trendy…), photo reprise d'un produit proche
		MenuItem mocha = created.stream().filter(i -> i.getName().equals("Ice Mocha")).findFirst().orElseThrow();
		assertEquals(24, mocha.getPrice());
		assertEquals(coldBrew.getPhoto(), mocha.getPhoto());
		assertTrue(created.stream().anyMatch(i -> i.getName().equals("Mango Pina Colada") && i.getPrice() == 28));
		// Un seul prix : la mention du grand format est retirée
		assertNull(chocolat.getPrice2());
		assertEquals("Chocolat chaud onctueux au lait.", chocolat.getDescription());
	}

	@Test
	void hotDrinksTakeTheBoardPriceOfTheIntensoOrSmallSize() {
		MenuItemRepository items = mock(MenuItemRepository.class);
		MenuCategory coffee = category("coffee", 0);
		MenuItem espresso = item(coffee, "Espresso", MenuSeeder.NEW_ESPRESSO, 8);
		MenuItem cortado = item(coffee, "Cortado", null, 12);
		MenuItem croissant = item(coffee, "Croissant pur beurre", null, 10);
		when(items.findAll()).thenReturn(List.of(espresso, cortado, croissant));
		new MenuSeeder(mock(MenuCategoryRepository.class), items, new ObjectMapper(), mock(AppSettingRepository.class)).applyHotBoardPrices();
		assertEquals(7, espresso.getPrice()); // prix de l'Intenso seulement
		assertNull(espresso.getPrice2());
		assertEquals(MenuSeeder.NEW_ESPRESSO, espresso.getDescription()); // toujours servi avec un verre d'eau
		assertEquals(18, cortado.getPrice());
		assertNull(cortado.getPrice2());
		assertEquals(10, croissant.getPrice());
	}

	@Test
	void eachNewDrinkGetsItsOwnPhotoUnlessTheAdminChoseOne() {
		MenuItemRepository items = mock(MenuItemRepository.class);
		MenuCategory hot = category("hot-coffee", 1);
		MenuItem chocolat = item(hot, "Chocolat chaud", null, 12);
		chocolat.update("Chocolat chaud", null, 12, null, null, "1618576230663-9714aecfb99a", false, null, true); // photo du Mocha
		MenuItem fondu = item(hot, "Chocolat fondu", null, 18);
		fondu.update("Chocolat fondu", null, 18, null, null, "https://exemple.ma/fondu.jpg", false, null, true); // choisie par l'admin
		when(items.findAll()).thenReturn(List.of(chocolat, fondu));
		new MenuSeeder(mock(MenuCategoryRepository.class), items, new ObjectMapper(), mock(AppSettingRepository.class)).applyOwnPhotos();
		assertEquals("1517578239113-b03992dcdd25", chocolat.getPhoto());
		assertEquals("https://exemple.ma/fondu.jpg", fondu.getPhoto());
	}
}
