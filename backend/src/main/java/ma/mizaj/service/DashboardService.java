package ma.mizaj.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import ma.mizaj.domain.Plan;
import ma.mizaj.domain.Redemption;
import ma.mizaj.domain.Subscription;
import ma.mizaj.domain.SubscriptionStatus;
import ma.mizaj.repo.CustomerRepository;
import ma.mizaj.repo.PlanRepository;
import ma.mizaj.repo.RedemptionRepository;
import ma.mizaj.repo.SubscriptionRepository;
import ma.mizaj.web.ApiException;
import ma.mizaj.web.dto.DashboardDtos.Dashboard;
import ma.mizaj.web.dto.DashboardDtos.Day;
import ma.mizaj.web.dto.DashboardDtos.FollowUp;
import ma.mizaj.web.dto.DashboardDtos.Kpis;
import ma.mizaj.web.dto.DashboardDtos.Month;
import ma.mizaj.web.dto.DashboardDtos.PlanStat;
import ma.mizaj.web.dto.DashboardDtos.ShopStat;
import ma.mizaj.web.dto.DashboardDtos.TopCustomer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tableau de bord des abonnements et de la consommation.
 * Les volumes d'un coffee shop restent modestes : les données sont chargées puis agrégées en mémoire.
 */
@Service
public class DashboardService {

	static final int MAX_DAYS = 366;
	static final int FOLLOW_UP_DAYS = 5;
	static final int FOLLOW_UP_DRINKS = 3;

	private final SubscriptionRepository subscriptions;
	private final RedemptionRepository redemptions;
	private final CustomerRepository customers;
	private final PlanRepository plans;
	private final Clock clock;
	private final ZoneId zone;

	public DashboardService(SubscriptionRepository subscriptions, RedemptionRepository redemptions,
			CustomerRepository customers, PlanRepository plans, Clock clock, ZoneId appZone) {
		this.subscriptions = subscriptions;
		this.redemptions = redemptions;
		this.customers = customers;
		this.plans = plans;
		this.clock = clock;
		this.zone = appZone;
	}

	@Transactional(readOnly = true)
	public Dashboard dashboard(LocalDate from, LocalDate to, String rawShop) {
		LocalDate today = LocalDate.now(clock.withZone(zone));
		if (to == null) {
			to = today;
		}
		if (from == null) {
			from = to.minusDays(29);
		}
		if (from.isAfter(to)) {
			throw ApiException.badRequest("La date de début doit précéder la date de fin.");
		}
		if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
			throw ApiException.badRequest("Période limitée à " + MAX_DAYS + " jours.");
		}
		String shop = rawShop == null || rawShop.isBlank() ? null : rawShop.trim();
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		LocalDate prevTo = from.minusDays(1);
		LocalDate prevFrom = prevTo.minusDays(days - 1);

		List<Subscription> allSubs = subscriptions.findAllWithCustomerAndPlan();
		Predicate<String> inShop = s -> shop == null || shop.equals(s);
		List<Subscription> subs = allSubs.stream().filter(s -> inShop.test(s.getShop())).toList();
		Instant start = startOf(from);
		Instant end = startOf(to.plusDays(1));
		// Une seule requête couvre la période et la période précédente (comparaison)
		List<Redemption> allReds = redemptions.findInPeriod(startOf(prevFrom), end).stream()
				.filter(r -> inShop.test(r.getShop())).toList();
		List<Redemption> reds = allReds.stream().filter(r -> !r.getRedeemedAt().isBefore(start)).toList();
		List<Redemption> prevReds = allReds.stream().filter(r -> r.getRedeemedAt().isBefore(start)).toList();

		Instant now = Instant.now(clock);
		Kpis kpis = kpis(allSubs, subs, reds, from, to, now);
		Kpis previous = kpis(allSubs, subs, prevReds, prevFrom, prevTo, now);

		TreeSet<String> shops = allSubs.stream().map(Subscription::getShop).filter(Objects::nonNull)
				.collect(Collectors.toCollection(TreeSet::new));
		reds.stream().map(Redemption::getShop).filter(Objects::nonNull).forEach(shops::add);

		return new Dashboard(from, to, shop, List.copyOf(shops), kpis, previous,
				daily(subs, reds, from, to),
				monthly(subs, to),
				planStats(subs, reds, from, to, now),
				shopStats(allSubs, start, end),
				heatmap(reds),
				followUp(allSubs, now),
				topCustomers(reds));
	}

	// --- Indicateurs ---

	private Kpis kpis(List<Subscription> allSubs, List<Subscription> subs, List<Redemption> reds, LocalDate from, LocalDate to,
			Instant now) {
		Instant start = startOf(from);
		Instant end = startOf(to.plusDays(1));
		List<Subscription> activated = between(subs, Subscription::getActivatedAt, start, end);
		Map<Long, Instant> firstActivation = firstActivationPerCustomer(allSubs);
		long first = activated.stream().filter(s -> s.getActivatedAt().equals(firstActivation.get(s.getCustomer().getId()))).count();
		long revenue = activated.stream().mapToLong(Subscription::getPricePaid).sum();
		long activeNow = subs.stream().filter(s -> s.statusAt(now) == SubscriptionStatus.ACTIVE).count();
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		List<Subscription> ended = endedBetween(subs, start, end, now);
		return new Kpis(revenue, activated.size(), first, activated.size() - first, reds.size(),
				customers.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end), activeNow,
				consumptionRate(ended), renewalRate(ended, allSubs), Math.round(reds.size() * 10.0 / days) / 10.0);
	}

	/**
	 * Périodes arrivées à échéance entre start et end (et déjà terminées) : base des taux de consommation et de
	 * renouvellement. Un renouvellement anticipé clôt la période à sa date : elle est comptée ce jour-là.
	 */
	private static List<Subscription> endedBetween(List<Subscription> subs, Instant start, Instant end, Instant now) {
		return subs.stream()
				.filter(s -> !s.getExpiresAt().isAfter(now))
				.filter(s -> !s.getExpiresAt().isBefore(start) && s.getExpiresAt().isBefore(end))
				.toList();
	}

	private static Double consumptionRate(List<Subscription> ended) {
		long bought = ended.stream().mapToLong(Subscription::getDrinksTotal).sum();
		if (bought == 0) {
			return null;
		}
		long consumed = ended.stream().mapToLong(s -> s.getDrinksTotal() - s.getDrinksLeft()).sum();
		return round(consumed * 100.0 / bought);
	}

	private static Double renewalRate(List<Subscription> ended, List<Subscription> allSubs) {
		if (ended.isEmpty()) {
			return null;
		}
		Map<Long, List<Instant>> activationsByCustomer = allSubs.stream().collect(Collectors.groupingBy(
				s -> s.getCustomer().getId(), Collectors.mapping(Subscription::getActivatedAt, Collectors.toList())));
		long renewed = ended.stream().filter(s -> activationsByCustomer.getOrDefault(s.getCustomer().getId(), List.of()).stream()
				.anyMatch(a -> a.isAfter(s.getActivatedAt()))).count();
		return round(renewed * 100.0 / ended.size());
	}

	// --- Séries ---

	private List<Day> daily(List<Subscription> subs, List<Redemption> reds, LocalDate from, LocalDate to) {
		Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
		for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
			byDay.put(d, new long[3]);
		}
		for (Redemption r : reds) {
			long[] v = byDay.get(dateOf(r.getRedeemedAt()));
			if (v != null) {
				v[0]++;
			}
		}
		for (Subscription s : subs) {
			long[] v = byDay.get(dateOf(s.getActivatedAt()));
			if (v != null) {
				v[1]++;
				v[2] += s.getPricePaid();
			}
		}
		return byDay.entrySet().stream().map(e -> new Day(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();
	}

	/** Chiffre d'affaires des 6 derniers mois (jusqu'au mois de fin de période), par formule. */
	private List<Month> monthly(List<Subscription> subs, LocalDate to) {
		YearMonth last = YearMonth.from(to);
		List<Month> months = new ArrayList<>();
		for (int i = 5; i >= 0; i--) {
			YearMonth ym = last.minusMonths(i);
			Instant start = startOf(ym.atDay(1));
			Instant end = startOf(ym.plusMonths(1).atDay(1));
			Map<String, Long> byPlan = new LinkedHashMap<>();
			long total = 0;
			for (Subscription s : between(subs, Subscription::getActivatedAt, start, end)) {
				String code = s.getPlan() == null ? "autre" : s.getPlan().getCode();
				byPlan.merge(code, (long) s.getPricePaid(), Long::sum);
				total += s.getPricePaid();
			}
			months.add(new Month(ym.toString(), total, byPlan));
		}
		return months;
	}

	private List<PlanStat> planStats(List<Subscription> subs, List<Redemption> reds, LocalDate from, LocalDate to, Instant now) {
		Instant start = startOf(from);
		Instant end = startOf(to.plusDays(1));
		List<PlanStat> out = new ArrayList<>();
		for (Plan p : plans.findAllByOrderBySortOrderAscIdAsc()) {
			Predicate<Subscription> ofPlan = s -> s.getPlan() != null && s.getPlan().getId().equals(p.getId());
			List<Subscription> planSubs = subs.stream().filter(ofPlan).toList();
			List<Subscription> activated = between(planSubs, Subscription::getActivatedAt, start, end);
			long active = planSubs.stream().filter(s -> s.statusAt(now) == SubscriptionStatus.ACTIVE).count();
			long planReds = reds.stream().filter(r -> ofPlan.test(r.getSubscription())).count();
			if (!p.isActive() && active == 0 && activated.isEmpty() && planReds == 0) {
				continue; // formule masquée, sans activité sur la période
			}
			out.add(new PlanStat(p.getCode(), p.getName(), p.getPrice(), active, activated.size(),
					activated.stream().mapToLong(Subscription::getPricePaid).sum(), planReds,
					consumptionRate(endedBetween(planSubs, start, end, now))));
		}
		return out;
	}

	/** Toujours tous les coffee shops (pour comparer), même quand un shop est filtré. */
	private List<ShopStat> shopStats(List<Subscription> allSubs, Instant start, Instant end) {
		Map<String, long[]> byShop = new HashMap<>();
		for (Subscription s : between(allSubs, Subscription::getActivatedAt, start, end)) {
			long[] v = byShop.computeIfAbsent(label(s.getShop()), k -> new long[3]);
			v[1]++;
			v[2] += s.getPricePaid();
		}
		for (Redemption r : redemptions.findInPeriod(start, end)) {
			byShop.computeIfAbsent(label(r.getShop()), k -> new long[3])[0]++;
		}
		return byShop.entrySet().stream()
				.map(e -> new ShopStat(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
				.sorted(Comparator.comparingLong(ShopStat::redemptions).reversed())
				.toList();
	}

	/** heatmap[jour 0 = lundi … 6 = dimanche][heure 0…23] = nombre de boissons validées. */
	private int[][] heatmap(List<Redemption> reds) {
		int[][] grid = new int[7][24];
		for (Redemption r : reds) {
			ZonedDateTime t = r.getRedeemedAt().atZone(zone);
			grid[t.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue()][t.getHour()]++;
		}
		return grid;
	}

	/** Abonnements actifs à relancer : fin proche ou presque épuisés (dernière période de chaque client). */
	private List<FollowUp> followUp(List<Subscription> allSubs, Instant now) {
		Map<Long, Subscription> latest = new HashMap<>();
		for (Subscription s : allSubs) {
			latest.merge(s.getCustomer().getId(), s, (a, b) -> a.getActivatedAt().isAfter(b.getActivatedAt()) ? a : b);
		}
		Instant soon = now.plus(Duration.ofDays(FOLLOW_UP_DAYS));
		return latest.values().stream()
				.filter(s -> s.statusAt(now) == SubscriptionStatus.ACTIVE)
				.filter(s -> s.getExpiresAt().isBefore(soon) || s.getDrinksLeft() <= FOLLOW_UP_DRINKS)
				.sorted(Comparator.comparing(Subscription::getExpiresAt))
				.map(s -> new FollowUp(s.getCustomer().getId(), s.getCustomer().getFirstName(),
						PhoneNumbers.pretty(s.getCustomer().getPhone()), s.getPlan() == null ? null : s.getPlan().getName(),
						s.getDrinksLeft(), s.getDrinksTotal(), s.getExpiresAt(),
						s.getExpiresAt().isBefore(soon) ? "EXPIRING" : "LOW"))
				.toList();
	}

	private List<TopCustomer> topCustomers(List<Redemption> reds) {
		Map<Long, List<Redemption>> byCustomer = reds.stream().collect(Collectors.groupingBy(r -> r.getSubscription().getCustomer().getId()));
		return byCustomer.values().stream()
				.sorted(Comparator.comparingInt((List<Redemption> l) -> l.size()).reversed())
				.limit(10)
				.map(l -> {
					var last = l.get(l.size() - 1).getSubscription();
					return new TopCustomer(last.getCustomer().getId(), last.getCustomer().getFirstName(),
							PhoneNumbers.pretty(last.getCustomer().getPhone()), last.getPlan() == null ? null : last.getPlan().getName(),
							l.size());
				})
				.toList();
	}

	// --- Outils ---

	private static Map<Long, Instant> firstActivationPerCustomer(List<Subscription> allSubs) {
		Map<Long, Instant> first = new HashMap<>();
		for (Subscription s : allSubs) {
			first.merge(s.getCustomer().getId(), s.getActivatedAt(), (a, b) -> a.isBefore(b) ? a : b);
		}
		return first;
	}

	private static List<Subscription> between(List<Subscription> subs, Function<Subscription, Instant> date,
			Instant start, Instant end) {
		return subs.stream().filter(s -> !date.apply(s).isBefore(start) && date.apply(s).isBefore(end)).toList();
	}

	private static String label(String shop) {
		return shop == null ? "Sans coffee shop" : shop;
	}

	private Instant startOf(LocalDate d) {
		return d.atStartOfDay(zone).toInstant();
	}

	private LocalDate dateOf(Instant i) {
		return i.atZone(zone).toLocalDate();
	}

	private static double round(double v) {
		return Math.round(v * 10) / 10.0;
	}
}
