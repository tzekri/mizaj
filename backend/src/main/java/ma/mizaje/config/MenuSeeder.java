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
			applyMenuBoard();
			applyColdBoard();
			applyHotBoardPrices();
			applyOwnPhotos();
			applyColdBoardPrices();
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
		applyMenuBoard();
		applyColdBoard();
		applyHotBoardPrices();
		applyOwnPhotos();
		applyColdBoardPrices();
	}

	/** Drapeau en base : prix du tableau imposés aux boissons froides (novembre 2026), une seule fois. */
	static final String COLD_PRICES_2026_11 = "menu.cold-prices.2026-11";

	/**
	 * Boissons froides aux prix du tableau, même celles qui étaient encore proposées à leur ancien prix (le gérant veut les mêmes
	 * prix partout). Une seule fois : un prix changé ensuite dans l'administration reste.
	 */
	void applyColdBoardPrices() {
		if (settings.existsById(COLD_PRICES_2026_11)) {
			return;
		}
		settings.save(new AppSetting(COLD_PRICES_2026_11, "done"));
		Map<String, Integer> prices = new java.util.HashMap<>();
		COLD_BOARD.forEach(c -> c.items().forEach(ci -> prices.put(ci.names().get(0), ci.price())));
		for (MenuItem i : items.findAll()) {
			Integer price = prices.get(i.getName());
			if (price != null && price != i.getPrice()) {
				log.info("Prix de {} : {} → {} DH", i.getName(), i.getPrice(), price);
				i.update(i.getName(), i.getDescription(), price, i.getIntensity(), i.getVolume(), i.getPhoto(), i.isFeatured(), i.getTag(),
						i.isAvailable());
				i.setSecondPrice(null, null);
			}
		}
	}

	/** Drapeau en base : une photo propre à chaque nouvelle boisson (novembre 2026), une seule fois. */
	static final String OWN_PHOTOS_2026_11 = "menu.own-photos.2026-11";

	/** Nouvelles boissons créées avec la photo d'une boisson proche : nom → {photo empruntée, photo à elle}. */
	static final Map<String, String[]> OWN_PHOTOS = Map.ofEntries(
			Map.entry("Double Espresso", new String[] {"1612183515105-8c737ba7585d", "1775134734575-58b2b65f1811"}),
			Map.entry("Pistachio Latte", new String[] {"1763208387697-fe0a913bfed9", "1766426910014-63a302fa137e"}),
			Map.entry("Chocolat chaud", new String[] {"1618576230663-9714aecfb99a", "1517578239113-b03992dcdd25"}),
			Map.entry("Chocolat fondu", new String[] {"1618576230663-9714aecfb99a", "1542990253-0d0f5be5f0ed"}),
			Map.entry("Ice Ubé Latte", new String[] {"1595977514600-72cbc8376c38", "1542518392-13317b1ee2a2"}),
			Map.entry("Ice Ubé & Cerise", new String[] {"1595977514600-72cbc8376c38", "1676566352352-40a5e9ae5e9b"}),
			Map.entry("Ice Strawberry Matcha", new String[] {"1717603545560-28f9da83ecc3", "1751033390130-cdc5a6082089"}),
			Map.entry("Ice Mango Matcha", new String[] {"1717603545560-28f9da83ecc3", "1749104028327-a33087ea4f47"}),
			Map.entry("Caramel Latte Frappé", new String[] {"1579888071069-c107a6f79d82", "1637178035575-b0dcbd28a09b"}),
			Map.entry("Mocha Frappé", new String[] {"1577805947697-89e18249d767", "1696487773677-c0c8061fe3d2"}),
			Map.entry("Pistachio Frappé", new String[] {"1568901839119-631418a3910d", "1555411093-41f7864ed3a2"}),
			Map.entry("Caramel Milkshake", new String[] {"1568901839119-631418a3910d", "1553787499-6f9133860278"}),
			Map.entry("Pina Colada", new String[] {"1613478223719-2ab802602423", "1607446045710-d5a8fd9bc1db"}),
			Map.entry("Mango Pina Colada", new String[] {"1613478223719-2ab802602423", "1623065422902-30a2d299bbe4"}));

	/** Chaque boisson a sa photo : seulement si elle a encore la photo empruntée (une photo choisie dans l'administration reste). */
	void applyOwnPhotos() {
		if (settings.existsById(OWN_PHOTOS_2026_11)) {
			return;
		}
		settings.save(new AppSetting(OWN_PHOTOS_2026_11, "done"));
		for (MenuItem i : items.findAll()) {
			String[] p = OWN_PHOTOS.get(i.getName());
			if (p != null && p[0].equals(i.getPhoto())) {
				i.update(i.getName(), i.getDescription(), i.getPrice(), i.getIntensity(), i.getVolume(), p[1], i.isFeatured(), i.getTag(),
						i.isAvailable());
			}
		}
	}

	/** Drapeau en base : prix du tableau des cafés chauds (novembre 2026), une seule fois. */
	static final String HOT_PRICES_2026_11 = "menu.hot-prices.2026-11";

	/** Prix du tableau des cafés chauds (un seul prix : Intenso ou petit format), par nom. */
	static final Map<String, Integer> HOT_PRICES = Map.ofEntries(
			Map.entry("Espresso", 7),
			Map.entry("Americano", 8),
			Map.entry("Double Espresso", 14),
			Map.entry("Latte", 11),
			Map.entry("Cappuccino", 11),
			Map.entry("Cortado", 18),
			Map.entry("Flat White", 20),
			Map.entry("Mocha", 15),
			Map.entry("Chocolat chaud", 12),
			Map.entry("Chocolat fondu", 18),
			Map.entry("Spanish Latte", 20),
			Map.entry("Pistachio Latte", 24),
			Map.entry("Matcha Latte", 28),
			Map.entry("Ubé Latte", 30));

	/** Cafés chauds aux prix du tableau (demandé par le gérant), une seule fois : un prix changé ensuite dans l'administration reste. */
	void applyHotBoardPrices() {
		if (settings.existsById(HOT_PRICES_2026_11)) {
			return;
		}
		settings.save(new AppSetting(HOT_PRICES_2026_11, "done"));
		for (MenuItem i : items.findAll()) {
			Integer price = HOT_PRICES.get(i.getName());
			if (price == null) {
				continue;
			}
			i.update(i.getName(), i.getDescription(), price, i.getIntensity(), i.getVolume(), i.getPhoto(), i.isFeatured(), i.getTag(),
					i.isAvailable());
			i.setSecondPrice(null, null);
			log.info("Prix de {} : {} DH", i.getName(), price);
		}
	}

	/** Drapeau en base : boissons froides du 2e tableau (novembre 2026), une seule fois. */
	static final String COLD_BOARD_2026_11 = "menu.cold-board.2026-11";

	/**
	 * Boisson froide du tableau. names : nom affiché puis anciens noms ; photo : identifiant Unsplash, ou nom d'un produit
	 * dont reprendre la photo.
	 */
	record ColdItem(List<String> names, int price, String description, String photo) {
		ColdItem(String name, int price) {
			this(List.of(name), price, null, null);
		}
	}

	/** Catégorie du tableau des boissons froides (créée si elle n'existe pas). */
	record ColdCategory(String slug, String light, String bold, String arabic, MenuTone tone, String photo, List<ColdItem> items) {
	}

	static final List<ColdCategory> COLD_BOARD = List.of(
			new ColdCategory("ice-coffee", "Ice", "Café", "قهوة مثلجة", MenuTone.DARK, "1592663527359-cf6642f54cff", List.of(
					new ColdItem(List.of("Ice Americano", "Iced Americano"), 14, null, null),
					new ColdItem(List.of("Ice Caramel Latte", "Iced Caramel Macchiato"), 22, "Espresso, lait froid et caramel beurre salé.", null),
					new ColdItem(List.of("Ice Latte Signature", "Iced Latte"), 25, "Vanille & coco.", null),
					new ColdItem(List.of("Ice Spanish Latte", "Iced Spanish Latte"), 24, null, null),
					new ColdItem(List.of("Ice Mocha"), 24, "Espresso, chocolat et lait froid.", "Cold Brew"))),
			new ColdCategory("ice-trendy", "Ice", "Trendy", "مشروبات عصرية", MenuTone.ACCENT, "1717603545560-28f9da83ecc3", List.of(
					new ColdItem(List.of("Ice Ubé Latte"), 30, "Ubé (igname violette) et lait froid.", "Purple Kidima"),
					new ColdItem(List.of("Ice Ubé & Cerise"), 35, "Ubé, coulis de cerise et lait froid.", "Purple Kidima"),
					new ColdItem(List.of("Ice Matcha Latte", "Iced Matcha"), 30, null, null),
					new ColdItem(List.of("Ice Strawberry Matcha"), 35, "Matcha, fraise écrasée et lait froid.", "Iced Matcha"),
					new ColdItem(List.of("Ice Mango Matcha"), 35, "Matcha, purée de mangue et lait froid.", "Iced Matcha"))),
			new ColdCategory("frappe", "Frappé", "by Kidima", "فرابيه", MenuTone.CREAM, "1579888071069-c107a6f79d82", List.of(
					new ColdItem(List.of("Spanish Latte Frappé"), 24, "Café frappé au lait concentré, chantilly.", "Frappé Caramel"),
					new ColdItem(List.of("Caramel Latte Frappé", "Frappé Caramel"), 24, null, null),
					new ColdItem(List.of("Mocha Frappé"), 24, "Café frappé au chocolat, chantilly.", "Milkshake Chocolat"),
					new ColdItem(List.of("Gourmet Belgium Chocolat"), 24, "Chocolat belge frappé, copeaux de chocolat.", "Milkshake Brownie"),
					new ColdItem(List.of("Pistachio Frappé"), 30, "Frappé à la crème de pistache.", "Milkshake Vanille"))),
			new ColdCategory("cocktail", "Fresh", "Cocktails", "كوكتيلات", MenuTone.ACCENT, "1654074517750-f854f7c27d62", List.of(
					new ColdItem(List.of("Fresh"), 20, "Menthe, citron et sprite.", "Citronnade Menthe"),
					new ColdItem(List.of("Mojito Virgin", "Virgin Mojito"), 19, null, null),
					new ColdItem(List.of("Mojito Fruités"), 24, "Au choix : fraise, fruits rouges, passion…", "Red Berries"),
					new ColdItem(List.of("Coco Passion"), 24, "Coco et fruit de la passion.", "Jus d’orange pressé"),
					new ColdItem(List.of("Pina Colada"), 24, "Ananas et coco.", "Jus d’orange pressé"),
					new ColdItem(List.of("Mango Pina Colada"), 28, "Mangue, ananas et coco.", "Jus d’orange pressé"),
					new ColdItem(List.of("Ice Tea", "Ice Tea Pêche"), 24, "Au choix : pêche, citron, fruits rouges.", null))),
			new ColdCategory("milkshake", "Fresh", "Milkshake", "ميلك شيك", MenuTone.CREAM, "1577805947697-89e18249d767", List.of(
					new ColdItem(List.of("Vanille Milkshake", "Milkshake Vanille"), 25, null, null),
					new ColdItem(List.of("Caramel Milkshake"), 25, "Glace vanille, lait et caramel.", "Milkshake Vanille"),
					new ColdItem(List.of("Chocolat Milkshake", "Milkshake Chocolat"), 25, null, null),
					new ColdItem(List.of("Fraise Milkshake", "Milkshake Fraise"), 25, null, null))));

	/**
	 * Boissons froides du 2e tableau : catégories Ice Café, Ice Trendy, Frappé, Cocktails, Milkshake affichées, dans cet ordre
	 * après les cafés chauds. Les produits déjà en base (masqués) sont réactivés, renommés et prennent le prix du tableau ;
	 * les autres sont créés. Les produits froids hors tableau restent masqués. Une seule fois.
	 */
	void applyColdBoard() {
		if (settings.existsById(COLD_BOARD_2026_11)) {
			return;
		}
		settings.save(new AppSetting(COLD_BOARD_2026_11, "done"));
		List<MenuCategory> allCategories = new java.util.ArrayList<>(categories.findAllByOrderBySortOrderAscIdAsc());
		List<MenuItem> all = new java.util.ArrayList<>(items.findAll());
		// Photos d'origine, relevées avant les renommages
		Map<String, String> photos = new java.util.HashMap<>();
		all.forEach(i -> photos.putIfAbsent(i.getName(), i.getPhoto()));
		// Après les cafés chauds visibles
		int order = (int) allCategories.stream().filter(MenuCategory::isVisible).count();
		java.util.Set<MenuItem> kept = new java.util.HashSet<>();
		java.util.Set<MenuCategory> coldCategories = new java.util.HashSet<>();
		for (ColdCategory cc : COLD_BOARD) {
			MenuCategory c = allCategories.stream().filter(x -> x.getSlug().equals(cc.slug())).findFirst().orElse(null);
			if (c == null) {
				c = new MenuCategory(cc.slug(), order);
				c.update(cc.light(), cc.bold(), cc.arabic(), cc.tone(), cc.photo(), true);
				c = categories.save(c);
				allCategories.add(c);
			}
			c.update(cc.light(), cc.bold(), cc.arabic(), cc.tone(), c.getPhoto(), true);
			c.setSortOrder(order++);
			coldCategories.add(c);
			for (int k = 0; k < cc.items().size(); k++) {
				ColdItem ci = cc.items().get(k);
				MenuItem item = all.stream().filter(i -> ci.names().contains(i.getName())).findFirst().orElse(null);
				String photo = item != null ? item.getPhoto()
						: ci.photo() == null ? cc.photo() : photos.getOrDefault(ci.photo(), ci.photo().matches("[0-9a-f-]{10,}") ? ci.photo() : cc.photo());
				// Un produit encore proposé garde son prix ; un produit masqué ou nouveau prend le prix du tableau
				int price = item != null && item.isAvailable() ? item.getPrice() : ci.price();
				String description = ci.description() != null ? ci.description() : item == null ? null : item.getDescription();
				if (item == null) {
					item = new MenuItem(c, k);
					item.update(ci.names().get(0), description, price, null, null, photo, false, null, true);
					items.save(item);
					all.add(item);
					log.info("Menu : {} ajouté ({} DH)", ci.names().get(0), ci.price());
				}
				item.update(ci.names().get(0), description, price, null, item.getVolume(), photo, item.isFeatured(), item.getTag(), true);
				item.setCategory(c);
				item.setSortOrder(k);
				kept.add(item);
			}
		}
		// Dans ces catégories, ce qui n'est pas sur le tableau reste masqué
		for (MenuItem i : all) {
			if (!kept.contains(i) && coldCategories.contains(i.getCategory()) && i.isAvailable()) {
				i.update(i.getName(), i.getDescription(), i.getPrice(), i.getIntensity(), i.getVolume(), i.getPhoto(), i.isFeatured(), i.getTag(),
						false);
			}
		}
		// Un seul prix affiché (Intenso / petit format) : la mention du second prix est retirée des descriptions
		description(all, "Double Espresso", "Double dose de notre assemblage signature.");
		description(all, "Chocolat chaud", "Chocolat chaud onctueux au lait.");
		log.info("Boissons froides du tableau : {} produits dans {} catégories.", kept.size(), COLD_BOARD.size());
	}

	private static void description(List<MenuItem> all, String name, String description) {
		all.stream().filter(i -> i.getName().equals(name)).findFirst().ifPresent(i -> i.update(i.getName(), description, i.getPrice(),
				i.getIntensity(), i.getVolume(), i.getPhoto(), i.isFeatured(), i.getTag(), i.isAvailable()));
	}

	/** Drapeau en base : menu ramené aux boissons du tableau (novembre 2026), une seule fois. */
	static final String BOARD_2026_11 = "menu.board.2026-11";

	/**
	 * Boisson du tableau. names : nom affiché puis anciens noms possibles ; si elle n'existe pas encore, elle est créée
	 * avec ce prix, cette description, ce volume et la photo du produit {@code photoOf}.
	 */
	record BoardItem(List<String> names, int price, String description, String volume, Integer intensity, String photoOf) {
		BoardItem(String name, String... formerNames) {
			this(java.util.stream.Stream.concat(java.util.stream.Stream.of(name), java.util.stream.Stream.of(formerNames)).toList(), 0, null,
					null, null, null);
		}
	}

	/** Le menu du tableau, catégorie par catégorie, dans l'ordre d'affichage. */
	static final Map<String, List<BoardItem>> BOARD = new java.util.LinkedHashMap<>();
	static {
		BOARD.put("coffee", List.of(
				new BoardItem("Espresso"),
				new BoardItem("Americano", "Américano"),
				new BoardItem(List.of("Double Espresso"), 14, "Double dose de notre assemblage signature. Intenso 14 DH · Dolce crema 18 DH.",
						"60 ml", 5, "Espresso"),
				new BoardItem("Cortado")));
		BOARD.put("hot-coffee", List.of(
				new BoardItem("Latte", "Café Latte"),
				new BoardItem("Cappuccino"),
				new BoardItem("Flat White"),
				new BoardItem("Mocha", "Mocaccino"),
				new BoardItem(List.of("Chocolat chaud"), 12, "Chocolat chaud onctueux au lait. Grand (L) : 16 DH.", "250 ml", null, "Mocha"),
				new BoardItem(List.of("Chocolat fondu"), 18, "Chocolat fondu, épais et gourmand, à la cuillère.", "200 ml", null, "Mocha"),
				new BoardItem("Spanish Latte"),
				new BoardItem(List.of("Pistachio Latte"), 24, "Latte à la crème de pistache, doux et parfumé.", "250 ml", null, "Latte"),
				new BoardItem("Matcha Latte"),
				new BoardItem(List.of("Ubé Latte"), 30, "Latte à l’ubé, l’igname violette : doux, vanillé, couleur lavande.", "250 ml", null,
						"Purple Kidima")));
	}

	/**
	 * Le menu ne garde que les boissons du tableau : elles sont activées (créées si besoin, renommées comme sur le tableau,
	 * prix existants conservés) ; les autres produits et catégories sont masqués, jamais supprimés. Une seule fois : ce que
	 * l'administrateur réactive ensuite reste réactivé.
	 */
	void applyMenuBoard() {
		if (settings.existsById(BOARD_2026_11)) {
			return;
		}
		settings.save(new AppSetting(BOARD_2026_11, "done"));
		List<MenuItem> all = new java.util.ArrayList<>(items.findAll());
		java.util.Set<MenuItem> kept = new java.util.HashSet<>();
		int order = 0;
		for (MenuCategory c : categories.findAllByOrderBySortOrderAscIdAsc()) {
			List<BoardItem> wanted = BOARD.get(c.getSlug());
			c.update(c.getLight(), c.getBold(), c.getArabic(), c.getTone(), c.getPhoto(), wanted != null);
			if (wanted == null) {
				continue;
			}
			c.setSortOrder(order++);
			for (int k = 0; k < wanted.size(); k++) {
				BoardItem b = wanted.get(k);
				MenuItem item = all.stream().filter(i -> b.names().contains(i.getName())).findFirst().orElse(null);
				if (item == null && b.price() == 0) {
					log.warn("Menu : {} introuvable (supprimé ?), à recréer dans l'administration.", b.names().get(0));
					continue;
				}
				if (item == null) {
					String photo = all.stream().filter(i -> i.getName().equals(b.photoOf()) || renamedFrom(i.getName(), b.photoOf()))
							.map(MenuItem::getPhoto).findFirst().orElse(c.getPhoto());
					item = new MenuItem(c, k);
					item.update(b.names().get(0), b.description(), b.price(), b.intensity(), b.volume(), photo, false, null, true);
					items.save(item);
					all.add(item);
					log.info("Menu : {} ajouté ({} DH)", b.names().get(0), b.price());
				} else {
					item.update(b.names().get(0), item.getDescription(), item.getPrice(), item.getIntensity(), item.getVolume(), item.getPhoto(),
							item.isFeatured(), item.getTag(), true);
					item.setCategory(c);
					item.setSortOrder(k);
				}
				kept.add(item);
			}
		}
		for (MenuItem i : all) {
			if (!kept.contains(i) && i.isAvailable()) {
				i.update(i.getName(), i.getDescription(), i.getPrice(), i.getIntensity(), i.getVolume(), i.getPhoto(), i.isFeatured(), i.getTag(),
						false);
			}
		}
		log.info("Menu ramené aux {} boissons du tableau ; autres produits et catégories masqués.", kept.size());
	}

	/** Le produit {@code name} porte-t-il un ancien nom de {@code boardName} (ex. « Mocaccino » pour « Mocha ») ? */
	private static boolean renamedFrom(String name, String boardName) {
		return BOARD.values().stream().flatMap(List::stream)
				.anyMatch(b -> b.names().get(0).equals(boardName) && b.names().contains(name));
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
