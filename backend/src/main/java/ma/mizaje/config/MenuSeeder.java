package ma.mizaje.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import ma.mizaje.domain.MenuCategory;
import ma.mizaje.domain.MenuItem;
import ma.mizaje.domain.MenuTone;
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

	public MenuSeeder(MenuCategoryRepository categories, MenuItemRepository items, ObjectMapper json) {
		this.categories = categories;
		this.items = items;
		this.json = json;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws IOException {
		if (categories.count() > 0) {
			renameLegacyBrand();
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

	static String rebrand(String text) {
		return text == null ? null : text.replaceAll("\\b(Mizaje?|Marokoffy)\\b", "Kidima");
	}
}
