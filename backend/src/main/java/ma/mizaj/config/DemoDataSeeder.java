package ma.mizaj.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import ma.mizaj.domain.Customer;
import ma.mizaj.domain.Plan;
import ma.mizaj.domain.Redemption;
import ma.mizaj.domain.StaffRole;
import ma.mizaj.domain.StaffUser;
import ma.mizaj.domain.Subscription;
import ma.mizaj.repo.CustomerRepository;
import ma.mizaj.repo.PlanRepository;
import ma.mizaj.repo.RedemptionRepository;
import ma.mizaj.repo.StaffUserRepository;
import ma.mizaj.repo.SubscriptionRepository;
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
 * Données fictives pour essayer le tableau de bord en local (APP_DEMO_DATA=true, activé par le profil « local »).
 * Jamais en production : ne s'exécute que si la base ne contient encore aucun client.
 * Environ 5 mois d'activité sur 3 coffee shops, avec des heures de pointe réalistes.
 */
@Component
@Order(10)
public class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
	private static final String[] NAMES = {"Yasmine", "Karim", "Salma", "Youssef", "Nadia", "Omar", "Imane", "Hamza", "Sara",
			"Mehdi", "Khadija", "Anas", "Hiba", "Reda", "Meryem", "Ayoub", "Lina", "Adam", "Ghita", "Ilyas", "Zineb", "Amine",
			"Rim", "Othmane", "Douae", "Soufiane", "Kenza", "Walid", "Aya", "Bilal"};
	private static final String[] SHOPS = {"Maârif", "Ain Diab", "Bourgogne"};
	/** Poids des heures 7h…22h (pic du matin, de midi et de fin d'après-midi). */
	private static final int[] HOUR_WEIGHTS = {6, 14, 12, 7, 5, 6, 9, 8, 5, 6, 8, 9, 6, 4, 3, 2};

	private final boolean enabled;
	private final CustomerRepository customers;
	private final SubscriptionRepository subscriptions;
	private final RedemptionRepository redemptions;
	private final StaffUserRepository staffUsers;
	private final PlanRepository plans;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final ZoneId zone;
	private final Random random = new Random(42);

	public DemoDataSeeder(@Value("${app.demo-data:false}") boolean enabled, CustomerRepository customers,
			SubscriptionRepository subscriptions, RedemptionRepository redemptions, StaffUserRepository staffUsers,
			PlanRepository plans, PasswordEncoder passwordEncoder, Clock clock, ZoneId appZone) {
		this.enabled = enabled;
		this.customers = customers;
		this.subscriptions = subscriptions;
		this.redemptions = redemptions;
		this.staffUsers = staffUsers;
		this.plans = plans;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.zone = appZone;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!enabled || customers.count() > 0) {
			return;
		}
		List<Plan> catalog = plans.findByActiveTrueOrderBySortOrderAscIdAsc();
		if (catalog.isEmpty()) {
			return;
		}
		List<StaffUser> baristas = new ArrayList<>();
		for (String shop : SHOPS) {
			String email = "barista." + shop.toLowerCase().replaceAll("[^a-z]", "") + "@mizajcoffee.ma";
			baristas.add(staffUsers.findByEmailIgnoreCase(email).orElseGet(() -> staffUsers.save(
					new StaffUser("Barista " + shop, email, passwordEncoder.encode("barista1234"), StaffRole.BARISTA, shop))));
		}

		Instant now = Instant.now(clock);
		LocalDate today = LocalDate.now(clock.withZone(zone));
		Set<String> phones = new HashSet<>();
		int subs = 0;
		int reds = 0;
		for (int i = 0; i < 70; i++) {
			String phone;
			do {
				phone = "+212" + (random.nextBoolean() ? "6" : "7") + (10_000_000 + random.nextInt(89_999_999));
			} while (!phones.add(phone));
			int shopIndex = weighted(new int[] {5, 3, 2});
			StaffUser barista = baristas.get(shopIndex);
			// Les inscriptions s'accélèrent : plus de clients récents que d'anciens
			int startDaysAgo = (int) Math.round(150 * Math.pow(random.nextDouble(), 1.4));
			Instant createdAt = at(today.minusDays(startDaysAgo), 8 + random.nextInt(10));
			Customer customer = customers.save(new Customer(phone, NAMES[random.nextInt(NAMES.length)], token(), createdAt));
			Plan plan = catalog.get(weighted(catalog.size() == 3 ? new int[] {2, 6, 3} : uniform(catalog.size())));
			customer.setPreferredPlan(plan);

			// Taux d'usage propre à chaque client (habitués vs occasionnels)
			double usage = 0.35 + random.nextDouble() * 0.6;
			Instant activation = createdAt.plus(Duration.ofHours(random.nextInt(48)));
			while (activation.isBefore(now)) {
				Subscription s = subscriptions.save(new Subscription(customer, plan, activation, barista));
				subs++;
				Instant periodEnd = s.getExpiresAt().isBefore(now) ? s.getExpiresAt() : now;
				long periodDays = Math.max(1, Duration.between(activation, periodEnd).toDays());
				int wanted = (int) Math.min(s.getDrinksTotal(), Math.round(s.getDrinksTotal() * usage * periodDays / plan.getDays()));
				List<Instant> times = new ArrayList<>();
				for (int k = 0; k < wanted; k++) {
					LocalDate day = activation.atZone(zone).toLocalDate().plusDays(1 + random.nextInt((int) periodDays));
					Instant t = at(day, 7 + weighted(HOUR_WEIGHTS));
					if (t.isAfter(activation) && t.isBefore(periodEnd)) {
						times.add(t);
					}
				}
				times.sort(null);
				for (Instant t : times) {
					s.consumeOne();
					redemptions.save(new Redemption(s, random.nextInt(5) == 0 ? baristas.get(random.nextInt(3)) : barista, t));
					reds++;
				}
				// Renouvellement : ~65 % des clients rechargent, parfois avec un peu de retard
				if (random.nextDouble() > 0.65) {
					break;
				}
				activation = s.getExpiresAt().plus(Duration.ofHours(random.nextInt(24 * 6)));
			}
		}
		log.info("Données de démonstration créées : 70 clients, {} abonnements, {} boissons.", subs, reds);
	}

	private Instant at(LocalDate day, int hour) {
		return day.atTime(hour, random.nextInt(60)).atZone(zone).toInstant();
	}

	private int weighted(int[] weights) {
		int total = 0;
		for (int w : weights) {
			total += w;
		}
		int r = random.nextInt(total);
		for (int i = 0; i < weights.length; i++) {
			r -= weights[i];
			if (r < 0) {
				return i;
			}
		}
		return weights.length - 1;
	}

	private static int[] uniform(int n) {
		int[] w = new int[n];
		Arrays.fill(w, 1);
		return w;
	}

	private String token() {
		String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
		StringBuilder sb = new StringBuilder(24);
		for (int i = 0; i < 24; i++) {
			sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return sb.toString();
	}
}
