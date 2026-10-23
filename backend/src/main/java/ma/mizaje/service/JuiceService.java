package ma.mizaje.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import ma.mizaje.domain.Customer;
import ma.mizaje.domain.JuiceCategory;
import ma.mizaje.domain.JuiceFavorite;
import ma.mizaje.domain.JuiceFormat;
import ma.mizaje.domain.JuiceIngredient;
import ma.mizaje.domain.JuiceOrder;
import ma.mizaje.domain.JuiceSize;
import ma.mizaje.domain.StaffUser;
import ma.mizaje.repo.CustomerRepository;
import ma.mizaje.repo.JuiceFavoriteRepository;
import ma.mizaje.repo.JuiceFormatRepository;
import ma.mizaje.repo.JuiceIngredientRepository;
import ma.mizaje.repo.JuiceOrderRepository;
import ma.mizaje.repo.StaffUserRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.JuiceDtos.Catalog;
import ma.mizaje.web.dto.JuiceDtos.FavoriteView;
import ma.mizaje.web.dto.JuiceDtos.FormatRequest;
import ma.mizaje.web.dto.JuiceDtos.IngredientRequest;
import ma.mizaje.web.dto.JuiceDtos.IngredientView;
import ma.mizaje.web.dto.JuiceDtos.JuiceOrderView;
import ma.mizaje.web.dto.JuiceDtos.JuiceStats;
import ma.mizaje.web.dto.JuiceDtos.RecipeRequest;
import ma.mizaje.web.dto.JuiceDtos.RecipeView;
import ma.mizaje.web.dto.JuiceDtos.SizeView;
import ma.mizaje.web.dto.JuiceDtos.TopMix;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composeur de jus : le client compose (1 base, 2 fruits et 2 extras au plus, 33 ou 50 cl), obtient un code
 * à présenter en caisse ; le barista le scanne, encaisse et le marque servi. Les abonnés gardent leurs favoris.
 */
@Service
public class JuiceService {

	static final int MAX_FRUITS = 2;
	static final int MAX_EXTRAS = 2;
	static final int MAX_FAVORITES = 10;
	/** Un code non utilisé expire au bout de ce délai (le client en recrée un en un geste). */
	static final Duration ORDER_VALIDITY = Duration.ofHours(48);
	public static final String QR_PREFIX = "MIZAJE-JUS:";
	/** Préfixe des codes générés des premières versions (ancien nom « Mizaj »), toujours accepté. */
	static final String LEGACY_QR_PREFIX = "MIZAJ-JUS:";
	private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

	private final JuiceIngredientRepository ingredients;
	private final JuiceFormatRepository formats;
	private final JuiceOrderRepository orders;
	private final JuiceFavoriteRepository favorites;
	private final CustomerRepository customers;
	private final StaffUserRepository staffUsers;
	private final Clock clock;
	private final ZoneId zone;
	private final SecureRandom random = new SecureRandom();

	public JuiceService(JuiceIngredientRepository ingredients, JuiceFormatRepository formats, JuiceOrderRepository orders, JuiceFavoriteRepository favorites,
			CustomerRepository customers, StaffUserRepository staffUsers, Clock clock, ZoneId appZone) {
		this.ingredients = ingredients;
		this.formats = formats;
		this.orders = orders;
		this.favorites = favorites;
		this.customers = customers;
		this.staffUsers = staffUsers;
		this.clock = clock;
		this.zone = appZone;
	}

	// --- Catalogue ---

	@Transactional(readOnly = true)
	public Catalog catalog() {
		List<IngredientView> available = ingredients.findAllByOrderByCategoryAscSortOrderAscIdAsc().stream()
				.filter(JuiceIngredient::isAvailable).map(IngredientView::of).toList();
		return new Catalog(available, sizes(), MAX_FRUITS, MAX_EXTRAS);
	}

	private List<SizeView> sizes() {
		Map<JuiceSize, JuiceFormat> byId = formats();
		return Arrays.stream(JuiceSize.values()).map(s -> {
			JuiceFormat f = byId.get(s);
			return new SizeView(s, s.label, f.getBasePrice(), f.getFactor());
		}).toList();
	}

	/** Prix de chaque format : réglages de l'administrateur, ou valeurs par défaut. */
	private Map<JuiceSize, JuiceFormat> formats() {
		Map<JuiceSize, JuiceFormat> byId = new java.util.EnumMap<>(JuiceSize.class);
		for (JuiceSize s : JuiceSize.values()) {
			byId.put(s, s.defaultFormat());
		}
		formats.findAll().forEach(f -> byId.put(f.getSize(), f));
		return byId;
	}

	// --- Commandes (code à présenter en caisse) ---

	@Transactional
	public JuiceOrderView order(RecipeRequest req, Long customerId) {
		Recipe recipe = resolve(req.ingredients(), req.size(), req.name(), true);
		Customer customer = customerId == null ? null : customers.findById(customerId).orElse(null);
		JuiceOrder order = orders.save(new JuiceOrder(newCode(), recipe.name(), recipe.size(), recipe.codes(), recipe.label(),
				recipe.signature(), recipe.price(), customer, Instant.now(clock)));
		return view(order);
	}

	@Transactional(readOnly = true)
	public JuiceOrderView byCode(String raw) {
		return view(load(raw));
	}

	/** Le barista a préparé et encaissé le jus. Un code ne sert qu'une fois. */
	@Transactional
	public JuiceOrderView serve(String raw, long staffId) {
		JuiceOrder order = load(raw);
		Instant now = Instant.now(clock);
		if (order.getServedAt() != null) {
			throw ApiException.conflict("ALREADY_SERVED", "Ce jus a déjà été servi (code utilisé).");
		}
		if (expired(order, now)) {
			throw ApiException.conflict("EXPIRED", "Ce code a expiré : le client peut recréer son jus en un geste.");
		}
		StaffUser staff = staffUsers.findById(staffId).filter(StaffUser::isActive).orElseThrow(ApiException::forbidden);
		order.serve(staff, now);
		return view(order);
	}

	// --- Favoris des abonnés ---

	@Transactional(readOnly = true)
	public List<FavoriteView> favorites(long customerId) {
		return favorites.findByCustomerOrderByCreatedAtDesc(loadCustomer(customerId)).stream().map(this::favoriteView).toList();
	}

	@Transactional
	public FavoriteView addFavorite(long customerId, RecipeRequest req) {
		Customer customer = loadCustomer(customerId);
		if (favorites.countByCustomer(customer) >= MAX_FAVORITES) {
			throw ApiException.conflict("TOO_MANY", "Vous avez déjà " + MAX_FAVORITES + " jus favoris : supprimez-en un pour en ajouter un nouveau.");
		}
		Recipe recipe = resolve(req.ingredients(), req.size(), req.name(), true);
		String name = recipe.name() == null ? "Mon jus Kidima" : recipe.name();
		return favoriteView(favorites.save(new JuiceFavorite(customer, name, recipe.size(), recipe.codes(), Instant.now(clock))));
	}

	@Transactional
	public void deleteFavorite(long customerId, long favoriteId) {
		favorites.delete(loadFavorite(customerId, favoriteId));
	}

	/** Recommander un favori : nouveau code, au prix actuel des ingrédients. */
	@Transactional
	public JuiceOrderView orderFavorite(long customerId, long favoriteId) {
		JuiceFavorite fav = loadFavorite(customerId, favoriteId);
		return order(new RecipeRequest(fav.getName(), fav.getSize(), List.of(fav.getIngredients().split("\\|"))), customerId);
	}

	// --- Administration ---

	@Transactional(readOnly = true)
	public List<IngredientView> allIngredients() {
		return ingredients.findAllByOrderByCategoryAscSortOrderAscIdAsc().stream().map(IngredientView::of).toList();
	}

	@Transactional
	public IngredientView createIngredient(IngredientRequest req) {
		String base = PlanService.slug(req.name());
		String code = base;
		for (int i = 2; ingredients.existsByCode(code); i++) {
			code = base + "-" + i;
		}
		JuiceIngredient ing = new JuiceIngredient(code, req.name().trim(), req.category(), req.price(), req.color().toLowerCase(),
				(int) ingredients.count() + 1);
		if (!req.available()) {
			ing.update(ing.getName(), ing.getCategory(), ing.getPrice(), ing.getColor(), false);
		}
		if (req.image() != null) { // absent : photo inchangée ; vide : photo retirée
			ing.setImage(req.image());
		}
		return IngredientView.of(ingredients.save(ing));
	}

	@Transactional
	public IngredientView updateIngredient(String code, IngredientRequest req) {
		JuiceIngredient ing = ingredients.findByCode(code).orElseThrow(() -> ApiException.notFound("Ingrédient introuvable."));
		ing.update(req.name().trim(), req.category(), req.price(), req.color().toLowerCase(), req.available());
		if (req.image() != null) { // absent : photo inchangée ; vide : photo retirée
			ing.setImage(req.image());
		}
		return IngredientView.of(ing);
	}

	/** Prix de départ et coefficient des suppléments d'un format (les codes déjà créés gardent leur prix). */
	@Transactional
	public List<SizeView> updateFormat(JuiceSize size, FormatRequest req) {
		JuiceFormat f = formats.findById(size).orElseGet(() -> formats.save(size.defaultFormat()));
		f.update(req.basePrice(), req.factor());
		return sizes();
	}

	@Transactional(readOnly = true)
	public List<SizeView> allSizes() {
		return sizes();
	}

	@Transactional(readOnly = true)
	public JuiceStats stats(LocalDate from, LocalDate to) {
		Instant start = from.atStartOfDay(zone).toInstant();
		Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
		List<JuiceOrder> served = orders.findByServedAtGreaterThanEqualAndServedAtLessThan(start, end);
		Map<String, List<JuiceOrder>> bySignature = served.stream().collect(Collectors.groupingBy(JuiceOrder::getSignature));
		List<TopMix> top = bySignature.values().stream()
				.sorted(Comparator.comparingInt((List<JuiceOrder> l) -> l.size()).reversed())
				.limit(5)
				.map(l -> new TopMix(l.get(0).getRecipeLabel(), l.get(0).getSize().label, l.size()))
				.toList();
		return new JuiceStats(served.size(), served.stream().mapToLong(JuiceOrder::getPrice).sum(),
				orders.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end), top);
	}

	// --- Recette ---

	/** Recette validée : ingrédients ordonnés (base, fruits, extras), libellé, signature et prix. */
	record Recipe(String name, JuiceSize size, List<JuiceIngredient> items, String codes, String label, String signature,
			int price, boolean available) {
	}

	/**
	 * @param strict true : refuse un ingrédient indisponible (commande) ; false : l'accepte (affichage d'un favori)
	 */
	Recipe resolve(List<String> rawCodes, JuiceSize size, String rawName, boolean strict) {
		if (size == null) {
			throw ApiException.badRequest("Choisissez un format (33 ou 50 cl).");
		}
		LinkedHashSet<String> codes = new LinkedHashSet<>();
		rawCodes.forEach(c -> codes.add(c == null ? "" : c.trim()));
		Map<String, JuiceIngredient> byCode = ingredients.findAll().stream()
				.collect(Collectors.toMap(JuiceIngredient::getCode, Function.identity()));
		List<JuiceIngredient> items = new ArrayList<>();
		boolean available = true;
		for (String code : codes) {
			JuiceIngredient ing = byCode.get(code);
			if (ing == null) {
				throw ApiException.badRequest("Ingrédient inconnu : " + code + ".");
			}
			if (!ing.isAvailable()) {
				if (strict) {
					throw ApiException.badRequest(ing.getName() + " n'est pas disponible en ce moment. Choisissez un autre ingrédient.");
				}
				available = false;
			}
			items.add(ing);
		}
		long bases = items.stream().filter(i -> i.getCategory() == JuiceCategory.BASE).count();
		long fruits = items.stream().filter(i -> i.getCategory() == JuiceCategory.FRUIT).count();
		long extras = items.stream().filter(i -> i.getCategory() == JuiceCategory.EXTRA).count();
		if (bases != 1) {
			throw ApiException.badRequest("Choisissez une base (et une seule) pour votre jus.");
		}
		if (fruits > MAX_FRUITS) {
			throw ApiException.badRequest(MAX_FRUITS + " fruits au maximum en plus de la base.");
		}
		if (extras > MAX_EXTRAS) {
			throw ApiException.badRequest(MAX_EXTRAS + " extras au maximum.");
		}
		items.sort(Comparator.comparing(JuiceIngredient::getCategory).thenComparing(JuiceIngredient::getSortOrder));
		String label = items.stream().map(JuiceIngredient::getName).collect(Collectors.joining(" · "));
		String joined = items.stream().map(JuiceIngredient::getCode).collect(Collectors.joining("|"));
		String signature = items.stream().map(JuiceIngredient::getCode).sorted().collect(Collectors.joining("+")) + "@" + size.name();
		int price = formats().get(size).price(items.stream().mapToInt(JuiceIngredient::getPrice).sum());
		String name = rawName == null || rawName.isBlank() ? null : rawName.trim();
		return new Recipe(name, size, items, joined, label, signature, price, available);
	}

	private RecipeView recipeView(Recipe r) {
		return new RecipeView(r.name(), r.size(), r.size().label, r.items().stream().map(IngredientView::of).toList(), r.label(),
				r.price(), r.available());
	}

	private JuiceOrderView view(JuiceOrder o) {
		Recipe recipe = resolve(List.of(o.getIngredients().split("\\|")), o.getSize(), o.getName(), false);
		// La commande garde son prix et son libellé d'origine (les tarifs peuvent avoir changé depuis)
		RecipeView rv = new RecipeView(o.getName(), o.getSize(), o.getSize().label,
				recipe.items().stream().map(IngredientView::of).toList(), o.getRecipeLabel(), o.getPrice(), true);
		Instant now = Instant.now(clock);
		String status = o.getServedAt() != null ? "SERVED" : expired(o, now) ? "EXPIRED" : "PENDING";
		return new JuiceOrderView(o.getCode(), QR_PREFIX + o.getCode(), rv, o.getCreatedAt(), o.getCreatedAt().plus(ORDER_VALIDITY),
				status, o.getServedAt(), o.getShop());
	}

	private FavoriteView favoriteView(JuiceFavorite f) {
		return new FavoriteView(f.getId(), recipeView(resolve(List.of(f.getIngredients().split("\\|")), f.getSize(), f.getName(), false)),
				f.getCreatedAt());
	}

	private static boolean expired(JuiceOrder o, Instant now) {
		return !o.getCreatedAt().plus(ORDER_VALIDITY).isAfter(now);
	}

	private JuiceOrder load(String raw) {
		String code = raw == null ? "" : raw.trim().toUpperCase();
		if (code.startsWith(QR_PREFIX)) {
			code = code.substring(QR_PREFIX.length());
		} else if (code.startsWith(LEGACY_QR_PREFIX)) {
			code = code.substring(LEGACY_QR_PREFIX.length());
		}
		code = code.replace("-", "").replace(" ", "");
		return orders.findByCode(code).orElseThrow(() -> ApiException.notFound("Code de jus inconnu. Vérifiez les 6 caractères."));
	}

	private Customer loadCustomer(long id) {
		return customers.findById(id).orElseThrow(() -> ApiException.unauthorized("Compte introuvable, reconnectez-vous."));
	}

	private JuiceFavorite loadFavorite(long customerId, long favoriteId) {
		return favorites.findByIdAndCustomer(favoriteId, loadCustomer(customerId))
				.orElseThrow(() -> ApiException.notFound("Favori introuvable."));
	}

	private String newCode() {
		String code;
		do {
			StringBuilder sb = new StringBuilder(6);
			for (int i = 0; i < 6; i++) {
				sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
			}
			code = sb.toString();
		} while (orders.existsByCode(code));
		return code;
	}
}
