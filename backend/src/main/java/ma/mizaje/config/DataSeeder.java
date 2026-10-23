package ma.mizaje.config;

import java.util.List;
import java.util.Map;
import ma.mizaje.domain.JuiceCategory;
import ma.mizaje.domain.JuiceIngredient;
import ma.mizaje.domain.Plan;
import ma.mizaje.domain.StaffRole;
import ma.mizaje.domain.StaffUser;
import ma.mizaje.repo.JuiceIngredientRepository;
import ma.mizaje.repo.PlanRepository;
import ma.mizaje.repo.StaffUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Au démarrage : formules d'abonnement de départ (si aucune n'existe), compte administrateur
 * et barista de démonstration s'ils n'existent pas encore.
 */
@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	static final String DEMO_BARISTA = "barista@kidima.ma";

	private final StaffUserRepository staffUsers;
	private final PlanRepository plans;
	private final JuiceIngredientRepository juiceIngredients;
	private final PasswordEncoder passwordEncoder;
	private final String adminEmail;
	private final String adminPassword;
	private final boolean seedDemo;

	public DataSeeder(StaffUserRepository staffUsers, PlanRepository plans, JuiceIngredientRepository juiceIngredients,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.email}") String adminEmail, @Value("${app.admin.password}") String adminPassword,
			@Value("${app.seed-demo}") boolean seedDemo) {
		this.staffUsers = staffUsers;
		this.plans = plans;
		this.juiceIngredients = juiceIngredients;
		this.passwordEncoder = passwordEncoder;
		this.adminEmail = adminEmail.trim().toLowerCase();
		this.adminPassword = adminPassword;
		this.seedDemo = seedDemo;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (plans.count() == 0) {
			plans.save(new Plan("decouverte", "Découverte", "Pour essayer, sans engagement", 45, 10, 15, "espresso", "espressos", 1));
			plans.save(new Plan("espresso", "Espresso", "Le classique : un espresso par jour", 120, 30, 30, "espresso", "espressos", 2));
			plans.save(new Plan("lait-douceur", "Lait & Douceur", "Cappuccino, latte, flat white, nouss-nouss…", 220, 30, 30,
					"boisson au lait", "boissons au lait", 3));
			log.info("Formules d'abonnement de départ créées (Découverte, Espresso, Lait & Douceur).");
		}
		if (juiceIngredients.count() == 0) {
			seedJuiceIngredients();
		}
		addMissingJuicePhotos();
		renameLegacyStaffEmails();
		if (adminEmail.isBlank() || adminPassword.length() < 8) {
			log.warn("ADMIN_EMAIL / ADMIN_PASSWORD (8 caractères min.) non renseignés : aucun compte administrateur créé.");
		} else if (!staffUsers.existsByEmailIgnoreCase(adminEmail)) {
			staffUsers.save(new StaffUser("Administrateur", adminEmail, passwordEncoder.encode(adminPassword), StaffRole.ADMIN, null));
			log.info("Compte administrateur créé : {}", adminEmail);
		}
		if (seedDemo && !staffUsers.existsByEmailIgnoreCase(DEMO_BARISTA)) {
			staffUsers.save(new StaffUser("Barista Démo", DEMO_BARISTA, passwordEncoder.encode("barista1234"), StaffRole.BARISTA, "Maârif"));
			log.info("Compte barista de démonstration créé : {} / barista1234", DEMO_BARISTA);
		}
	}

	/** Anciens noms de domaine des comptes (Mizaj, Mizaje, Marokoffy) : ils deviennent …@kidima.ma. */
	private static final List<String> LEGACY_DOMAINS = List.of("@mizajcoffee.ma", "@mizajecoffee.ma", "@marokoffy.ma");

	/** Comptes créés sous un ancien nom de la marque : adresse renommée, mot de passe inchangé. */
	private void renameLegacyStaffEmails() {
		for (StaffUser u : staffUsers.findAll()) {
			String email = u.getEmail();
			for (String domain : LEGACY_DOMAINS) {
				if (email.toLowerCase().endsWith(domain)) {
					String renamed = email.substring(0, email.length() - domain.length()) + "@kidima.ma";
					if (!staffUsers.existsByEmailIgnoreCase(renamed)) {
						u.setEmail(renamed);
						log.info("Compte renommé : {} → {}", email, renamed);
					}
				}
			}
		}
	}

	/** Photos Unsplash (licence libre) des ingrédients de départ, par code. */
	static final Map<String, String> JUICE_PHOTOS = Map.ofEntries(
			Map.entry("orange", "1632371027159-3e411104c6ea"),
			Map.entry("citronnade", "1587486937303-32eaa2134b78"),
			Map.entry("pomme", "1705147296208-dffaf3fe5d7c"),
			Map.entry("ananas", "1490885578174-acda8905c2c6"),
			Map.entry("carotte", "1589927986089-35812388d1f4"),
			Map.entry("lait", "1602153508753-4ace888c10a0"),
			Map.entry("fraise", "1640958899516-c08a59225611"),
			Map.entry("mangue", "1553279768-865429fa0078"),
			Map.entry("banane", "1587132137056-bfbf0166836e"),
			Map.entry("avocat", "1519162808019-7de1683fa2ad"),
			Map.entry("kiwi", "1662303645991-185042acc773"),
			Map.entry("fruits-rouges", "1710528184650-fc75ae862c13"),
			Map.entry("pasteque", "1581074817932-af423ba4566e"),
			Map.entry("grenade", "1615485925600-97237c4fc1ec"),
			Map.entry("menthe", "1588908933351-eeb8cd4c4521"),
			Map.entry("gingembre", "1615484478243-c94e896edbae"),
			Map.entry("miel", "1655169947079-5b2a38815147"),
			Map.entry("citron-vert", "1619678544337-4a173360decd"),
			Map.entry("amandes", "1772986796415-d38ed846bca7"),
			Map.entry("fleur-oranger", "1780027069438-24abd628e0ba"));

	static String unsplash(String id) {
		return "https://images.unsplash.com/photo-" + id + "?auto=format&fit=crop&w=240&h=240&q=70";
	}

	/** Ajoute la photo par défaut aux ingrédients de départ qui n'en ont pas (bases créées avant l'ajout des photos). */
	private void addMissingJuicePhotos() {
		juiceIngredients.findAll().stream()
				.filter(i -> i.getImage() == null && JUICE_PHOTOS.containsKey(i.getCode()))
				.forEach(i -> i.setImage(unsplash(JUICE_PHOTOS.get(i.getCode()))));
	}

	/** Ingrédients de départ du composeur de jus (prix = supplément pour un 33 cl, modifiables dans l'administration). */
	private void seedJuiceIngredients() {
		Object[][] all = {
				{"orange", "Orange pressée", JuiceCategory.BASE, 0, "#f7a21b"},
				{"citronnade", "Citronnade", JuiceCategory.BASE, 0, "#ecd85a"},
				{"pomme", "Pomme", JuiceCategory.BASE, 3, "#c9d65a"},
				{"ananas", "Ananas", JuiceCategory.BASE, 4, "#f4c542"},
				{"carotte", "Carotte", JuiceCategory.BASE, 2, "#ef8a2a"},
				{"lait", "Lait (smoothie)", JuiceCategory.BASE, 2, "#f1e9db"},
				{"fraise", "Fraise", JuiceCategory.FRUIT, 5, "#e5414f"},
				{"mangue", "Mangue", JuiceCategory.FRUIT, 6, "#f9a825"},
				{"banane", "Banane", JuiceCategory.FRUIT, 4, "#f2e19c"},
				{"avocat", "Avocat", JuiceCategory.FRUIT, 8, "#93b44f"},
				{"kiwi", "Kiwi", JuiceCategory.FRUIT, 5, "#7cb342"},
				{"fruits-rouges", "Fruits rouges", JuiceCategory.FRUIT, 7, "#a8264a"},
				{"pasteque", "Pastèque", JuiceCategory.FRUIT, 4, "#ef6b6b"},
				{"grenade", "Grenade", JuiceCategory.FRUIT, 6, "#c2185b"},
				{"menthe", "Menthe fraîche", JuiceCategory.EXTRA, 2, "#3e9b5b"},
				{"gingembre", "Gingembre", JuiceCategory.EXTRA, 2, "#d8b26a"},
				{"miel", "Miel", JuiceCategory.EXTRA, 3, "#dfa530"},
				{"citron-vert", "Citron vert", JuiceCategory.EXTRA, 2, "#9ccc65"},
				{"amandes", "Amandes", JuiceCategory.EXTRA, 4, "#c8a27a"},
				{"fleur-oranger", "Fleur d'oranger", JuiceCategory.EXTRA, 2, "#f6eccd"},
		};
		int order = 1;
		for (Object[] i : all) {
			juiceIngredients.save(new JuiceIngredient((String) i[0], (String) i[1], (JuiceCategory) i[2], (Integer) i[3], (String) i[4], order++));
		}
		log.info("Ingrédients du composeur de jus créés ({}).", all.length);
	}
}
