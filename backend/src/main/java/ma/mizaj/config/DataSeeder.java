package ma.mizaj.config;

import ma.mizaj.domain.Plan;
import ma.mizaj.domain.StaffRole;
import ma.mizaj.domain.StaffUser;
import ma.mizaj.repo.PlanRepository;
import ma.mizaj.repo.StaffUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Au démarrage : formules d'abonnement de départ (si aucune n'existe), compte administrateur
 * et barista de démonstration s'ils n'existent pas encore.
 */
@Component
public class DataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	static final String DEMO_BARISTA = "barista@mizajcoffee.ma";

	private final StaffUserRepository staffUsers;
	private final PlanRepository plans;
	private final PasswordEncoder passwordEncoder;
	private final String adminEmail;
	private final String adminPassword;
	private final boolean seedDemo;

	public DataSeeder(StaffUserRepository staffUsers, PlanRepository plans, PasswordEncoder passwordEncoder,
			@Value("${app.admin.email}") String adminEmail, @Value("${app.admin.password}") String adminPassword,
			@Value("${app.seed-demo}") boolean seedDemo) {
		this.staffUsers = staffUsers;
		this.plans = plans;
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
}
