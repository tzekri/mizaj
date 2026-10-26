package ma.mizaje.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import ma.mizaje.domain.AppSetting;
import ma.mizaje.domain.MenuCategory;
import ma.mizaje.domain.MenuItem;
import ma.mizaje.domain.MenuTone;
import ma.mizaje.repo.AppSettingRepository;
import ma.mizaje.repo.MenuCategoryRepository;
import ma.mizaje.repo.MenuItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Au premier démarrage (menu vide) : charge le menu de départ (seed/menu.json), ensuite géré dans l'administration. */
@Component
@Order(2)
public class MenuSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MenuSeeder.class);
	static final String SEED = "seed/menu.json";

	record SeedItem(String name, String description, int price, Integer intensity, String volume, String photo, boolean featured,
			String tag) {
	}

	record SeedCategory(String slug, String light, String bold, String arabic, MenuTone tone, String photo, List<SeedItem> items) {
	}

	private final MenuCategoryRepository categories;
	private final MenuItemRepository items;
	private final ObjectMapper json;
	private final AppSettingRepository settings;

	public MenuSeeder(MenuCategoryRepository categories, MenuItemRepository items, ObjectMapper json, AppSettingRepository settings) {
		this.categories = categories;
		this.items = items;
		this.json = json;
		this.settings = settings;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws IOException {
		if (categories.count() > 0) {
			renameLegacyBrand();
			updateFormerPrices();
			return;
		}
		List<SeedCategory> seed;
		try (InputStream in = new ClassPathResource(SEED).getInputStream()) {
			seed = List.of(json.readValue(in, SeedCategory[].class));
		}
		int n = 0;
		for (int c = 0; c < seed.size(); c++) {
			SeedCategory sc = seed.get(c);
			MenuCategory category = new MenuCategory(sc.slug(), c);
			category.update(sc.light(), sc.bold(), sc.arabic(), sc.tone(), sc.photo(), true);
			categories.save(category);
			for (int i = 0; i < sc.items().size(); i++) {
				SeedItem si = sc.items().get(i);
				MenuItem item = new MenuItem(category, i);
				item.update(si.name(), si.description(), si.price(), si.intensity(), si.volume(), si.photo(), si.featured(), si.tag(), true);
				items.save(item);
				n++;
			}
		}
		log.info("Menu de départ chargé : {} catégories, {} produits.", seed.size(), n);
		settings.save(new AppSetting(PRICES_2026_10, "done")); // le menu de départ a déjà les nouveaux prix
	}

	/** Produits nommés d'après une ancienne marque (« Mizaje Signature », « Marokoffy Intense »…) : renommés « Kidima ». */
	private void renameLegacyBrand() {
		for (MenuItem i : items.findAll()) {
			String name = rebrand(i.getName());
			String description = rebrand(i.getDescription());
			if (!name.equals(i.getName()) || (description != null && !description.equals(i.getDescription()))) {
				log.info("Produit renommé : {} → {}", i.getName(), name);
				i.update(name, description, i.getPrice(), i.getIntensity(), i.getVolume(), i.getPhoto(), i.isFeatured(), i.getTag(),
						i.isAvailable());
			}
		}
	}

	/** Nouveaux prix (octobre 2026, espresso à 8 DH avec un verre d'eau) : nom → {ancien prix, nouveau prix}. */
	static final Map<String, int[]> NEW_PRICES = Map.ofEntries(
			Map.entry("Espresso", new int[] {10, 8}),
			Map.entry("Ristretto", new int[] {12, 8}),
			Map.entry("Américano", new int[] {14, 10}),
			Map.entry("Macchiato", new int[] {13, 10}),
			Map.entry("Cortado", new int[] {16, 12}),
			Map.entry("Café turc", new int[] {20, 15}),
			Map.entry("Cappuccino", new int[] {20, 15}),
			Map.entry("Café Latte", new int[] {22, 16}),
			Map.entry("Flat White", new int[] {22, 18}),
			Map.entry("Spanish Latte", new int[] {28, 22}),
			Map.entry("Caramel Latte", new int[] {26, 20}),
			Map.entry("Mocaccino", new int[] {26, 20}),
			Map.entry("Nouss-Nouss", new int[] {12, 10}),
			Map.entry("Iced Latte", new int[] {24, 18}),
			Map.entry("Iced Spanish Latte", new int[] {30, 24}),
			Map.entry("Iced Caramel Macchiato", new int[] {30, 24}),
			Map.entry("Iced Americano", new int[] {20, 14}),
			Map.entry("Cold Brew", new int[] {25, 20}),
			Map.entry("Frappé Caramel", new int[] {32, 25}),
			Map.entry("Milkshake Oreo", new int[] {32, 25}),
			Map.entry("Milkshake Chocolat", new int[] {30, 23}),
			Map.entry("Milkshake Fraise", new int[] {30, 23}),
			Map.entry("Milkshake Vanille", new int[] {28, 22}),
			Map.entry("Milkshake Brownie", new int[] {35, 28}),
			Map.entry("Virgin Mojito", new int[] {28, 22}),
			Map.entry("Red Berries", new int[] {30, 24}),
			Map.entry("Purple Kidima", new int[] {32, 25}),
			Map.entry("Citronnade Menthe", new int[] {22, 15}),
			Map.entry("Jus d’orange pressé", new int[] {20, 15}),
			Map.entry("Atay à la menthe", new int[] {15, 10}),
			Map.entry("Atay Royal", new int[] {25, 18}),
			Map.entry("Thé noir", new int[] {15, 10}),
			Map.entry("Chai Latte", new int[] {24, 18}),
			Map.entry("Matcha Latte", new int[] {30, 25}),
			Map.entry("Infusion Bien-être", new int[] {15, 10}),
			Map.entry("Ice Tea Citron", new int[] {22, 16}),
			Map.entry("Ice Tea Pêche", new int[] {24, 18}),
			Map.entry("Ice Tea Fruits Rouges", new int[] {24, 18}),
			Map.entry("Hibiscus Glacé", new int[] {22, 16}),
			Map.entry("Iced Matcha", new int[] {32, 26}));
	static final String OLD_ESPRESSO = "Notre assemblage signature, corsé et intense, crema noisette.";
	static final String NEW_ESPRESSO = "Notre assemblage signature, corsé et intense, crema noisette. Servi avec un verre d’eau.";

	/** Drapeau en base : nouveaux prix déjà appliqués (ne pas écraser ensuite un prix choisi dans l'administration). */
	static final String PRICES_2026_10 = "menu.prices.2026-10";

	/** Applique les nouveaux prix une seule fois, et seulement aux produits dont l'administrateur n'a pas déjà changé le prix. */
	private void updateFormerPrices() {
		if (settings.existsById(PRICES_2026_10)) {
			return;
		}
		settings.save(new AppSetting(PRICES_2026_10, "done"));
		for (MenuItem i : items.findAll()) {
			int[] change = NEW_PRICES.get(i.getName());
			boolean price = change != null && i.getPrice() == change[0];
			boolean water = "Espresso".equals(i.getName()) && OLD_ESPRESSO.equals(i.getDescription());
			if (price || water) {
				i.update(i.getName(), water ? NEW_ESPRESSO : i.getDescription(), price ? change[1] : i.getPrice(), i.getIntensity(),
						i.getVolume(), i.getPhoto(), i.isFeatured(), i.getTag(), i.isAvailable());
				if (price) {
					log.info("Prix de {} : {} → {} DH", i.getName(), change[0], change[1]);
				}
			}
		}
	}

	static String rebrand(String text) {
		return text == null ? null : text.replaceAll("\\b(Mizaje?|Marokoffy)\\b", "Kidima");
	}
}
