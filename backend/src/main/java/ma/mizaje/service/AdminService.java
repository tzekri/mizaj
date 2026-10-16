package ma.mizaje.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import ma.mizaje.domain.StaffUser;
import ma.mizaje.repo.CustomerRepository;
import ma.mizaje.repo.RedemptionRepository;
import ma.mizaje.repo.StaffUserRepository;
import ma.mizaje.repo.SubscriptionRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.Dtos.CreateStaffRequest;
import ma.mizaje.web.dto.Dtos.StaffView;
import ma.mizaje.web.dto.Dtos.StatsView;
import ma.mizaje.web.dto.Dtos.SubscriberView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

	private final CustomerRepository customers;
	private final SubscriptionRepository subscriptions;
	private final RedemptionRepository redemptions;
	private final StaffUserRepository staffUsers;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final ZoneId zone;

	public AdminService(CustomerRepository customers, SubscriptionRepository subscriptions, RedemptionRepository redemptions,
			StaffUserRepository staffUsers, PasswordEncoder passwordEncoder, Clock clock, ZoneId appZone) {
		this.customers = customers;
		this.subscriptions = subscriptions;
		this.redemptions = redemptions;
		this.staffUsers = staffUsers;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.zone = appZone;
	}

	@Transactional(readOnly = true)
	public StatsView stats() {
		LocalDate today = LocalDate.now(clock.withZone(zone));
		Instant startOfDay = today.atStartOfDay(zone).toInstant();
		Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
		Instant now = Instant.now(clock);
		return new StatsView(
				customers.count(),
				subscriptions.countActive(now),
				subscriptions.revenueSince(startOfMonth),
				subscriptions.countByActivatedAtGreaterThanEqual(startOfMonth),
				redemptions.countByRedeemedAtGreaterThanEqualAndCancelledAtIsNull(startOfDay),
				redemptions.countByRedeemedAtGreaterThanEqualAndCancelledAtIsNull(startOfMonth));
	}

	/** Dernière période de chaque client ayant déjà été activé, les abonnements actifs d'abord. */
	@Transactional(readOnly = true)
	public List<SubscriberView> subscribers() {
		Instant now = Instant.now(clock);
		return subscriptions.findLatestPerCustomer().stream()
				.map(s -> new SubscriberView(s.getCustomer().getId(), s.getCustomer().getFirstName(),
						PhoneNumbers.pretty(s.getCustomer().getPhone()), s.getPlan() == null ? null : s.getPlan().getName(),
						s.statusAt(now), s.getDrinksLeft(), s.getExpiresAt()))
				.sorted(Comparator.comparing((SubscriberView v) -> v.status().ordinal()).thenComparing(SubscriberView::expiresAt,
						Comparator.reverseOrder()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<StaffView> staff() {
		return staffUsers.findAllByOrderByFullNameAsc().stream().map(AdminService::view).toList();
	}

	@Transactional
	public StaffView createStaff(CreateStaffRequest req) {
		String email = req.email().trim().toLowerCase();
		if (staffUsers.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("EMAIL_TAKEN", "Un compte existe déjà avec cet e-mail.");
		}
		String shop = req.shop() == null || req.shop().isBlank() ? null : req.shop().trim();
		return view(staffUsers.save(new StaffUser(req.fullName().trim(), email, passwordEncoder.encode(req.password()),
				req.role(), shop)));
	}

	@Transactional
	public StaffView setActive(long staffId, boolean active, long currentStaffId) {
		if (staffId == currentStaffId && !active) {
			throw ApiException.badRequest("Vous ne pouvez pas désactiver votre propre compte.");
		}
		StaffUser user = staffUsers.findById(staffId).orElseThrow(() -> ApiException.notFound("Compte introuvable."));
		user.setActive(active);
		return view(user);
	}

	private static StaffView view(StaffUser u) {
		return new StaffView(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getShop(), u.isActive());
	}
}
