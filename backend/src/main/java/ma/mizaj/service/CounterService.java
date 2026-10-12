package ma.mizaj.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import ma.mizaj.config.PlanProperties;
import ma.mizaj.domain.Customer;
import ma.mizaj.domain.Redemption;
import ma.mizaj.domain.StaffUser;
import ma.mizaj.domain.Subscription;
import ma.mizaj.domain.SubscriptionStatus;
import ma.mizaj.repo.CustomerRepository;
import ma.mizaj.repo.RedemptionRepository;
import ma.mizaj.repo.StaffUserRepository;
import ma.mizaj.repo.SubscriptionRepository;
import ma.mizaj.web.ApiException;
import ma.mizaj.web.dto.Dtos.PlanView;
import ma.mizaj.web.dto.Dtos.StaffCardView;
import ma.mizaj.web.dto.Dtos.TodayView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opérations du barista au comptoir : lecture de la carte, validation d'un espresso, activation après paiement. */
@Service
public class CounterService {

	/** En dessous de ce délai depuis le dernier espresso, une confirmation est demandée (double scan). */
	static final Duration DOUBLE_SCAN_WINDOW = Duration.ofMinutes(2);
	/** Une validation peut être annulée pendant ce délai (erreur de manipulation). */
	static final Duration UNDO_WINDOW = Duration.ofMinutes(10);
	static final String QR_PREFIX = "MIZAJ:";

	private final CustomerRepository customers;
	private final SubscriptionRepository subscriptions;
	private final RedemptionRepository redemptions;
	private final StaffUserRepository staffUsers;
	private final PlanProperties plan;
	private final Clock clock;
	private final ZoneId zone;

	public CounterService(CustomerRepository customers, SubscriptionRepository subscriptions,
			RedemptionRepository redemptions, StaffUserRepository staffUsers, PlanProperties plan, Clock clock, ZoneId appZone) {
		this.customers = customers;
		this.subscriptions = subscriptions;
		this.redemptions = redemptions;
		this.staffUsers = staffUsers;
		this.plan = plan;
		this.clock = clock;
		this.zone = appZone;
	}

	@Transactional(readOnly = true)
	public StaffCardView byToken(String scanned) {
		String token = scanned.trim();
		if (token.startsWith(QR_PREFIX)) {
			token = token.substring(QR_PREFIX.length());
		}
		Customer customer = customers.findByQrToken(token)
				.orElseThrow(() -> ApiException.notFound("QR code inconnu. Le client a peut-être régénéré sa carte : demandez-lui de rouvrir sa page « Ma carte »."));
		return view(customer);
	}

	@Transactional(readOnly = true)
	public StaffCardView byPhone(String rawPhone) {
		Customer customer = customers.findByPhone(PhoneNumbers.normalize(rawPhone))
				.orElseThrow(() -> ApiException.notFound("Aucun compte avec ce numéro. Le client doit d'abord s'inscrire sur la page Abonnement."));
		return view(customer);
	}

	@Transactional
	public StaffCardView redeem(long customerId, long staffId, boolean force) {
		Customer customer = loadCustomer(customerId);
		Instant now = Instant.now(clock);
		Subscription sub = subscriptions.findFirstByCustomerOrderByActivatedAtDesc(customer)
				.orElseThrow(() -> ApiException.conflict("NO_SUBSCRIPTION", "Ce client n'a pas encore d'abonnement actif. Encaissez " + plan.price() + " DH puis activez-le."));
		switch (sub.statusAt(now)) {
			case EXPIRED -> throw ApiException.conflict("EXPIRED", "Abonnement expiré. Proposez au client de le renouveler (" + plan.price() + " DH).");
			case EMPTY -> throw ApiException.conflict("EMPTY", "Les " + sub.getDrinksTotal() + " espressos de cet abonnement ont été consommés. Proposez le renouvellement.");
			default -> {
			}
		}
		if (!force) {
			redemptions.findFirstBySubscriptionAndCancelledAtIsNullOrderByRedeemedAtDesc(sub)
					.filter(r -> r.getRedeemedAt().isAfter(now.minus(DOUBLE_SCAN_WINDOW)))
					.ifPresent(r -> {
						long seconds = Duration.between(r.getRedeemedAt(), now).toSeconds();
						throw ApiException.conflict("RECENT", "Un espresso a déjà été validé sur cette carte il y a " + seconds + " s. Valider quand même ?");
					});
		}
		sub.consumeOne();
		redemptions.save(new Redemption(sub, loadStaff(staffId), now));
		return view(customer);
	}

	/** Le client a payé en caisse : nouvelle période de N espressos valables D jours. */
	@Transactional
	public StaffCardView activate(long customerId, long staffId, boolean confirmReplace) {
		Customer customer = loadCustomer(customerId);
		Instant now = Instant.now(clock);
		Optional<Subscription> current = subscriptions.findFirstByCustomerOrderByActivatedAtDesc(customer);
		if (current.isPresent() && current.get().statusAt(now) == SubscriptionStatus.ACTIVE) {
			if (!confirmReplace) {
				throw ApiException.conflict("STILL_ACTIVE", "L'abonnement en cours a encore " + current.get().getDrinksLeft()
						+ " espresso(s). Ils seront perdus (pas de report). Renouveler quand même ?");
			}
			current.get().closeAt(now);
		}
		subscriptions.save(new Subscription(customer, now, now.plus(Duration.ofDays(plan.days())), plan.drinks(), plan.price(),
				loadStaff(staffId)));
		return view(customer);
	}

	@Transactional
	public StaffCardView undo(long redemptionId) {
		Redemption redemption = redemptions.findById(redemptionId)
				.orElseThrow(() -> ApiException.notFound("Validation introuvable."));
		Instant now = Instant.now(clock);
		if (redemption.getCancelledAt() != null) {
			throw ApiException.conflict("ALREADY_CANCELLED", "Cette validation a déjà été annulée.");
		}
		if (redemption.getRedeemedAt().isBefore(now.minus(UNDO_WINDOW))) {
			throw ApiException.conflict("TOO_LATE", "Une validation ne peut être annulée que dans les " + UNDO_WINDOW.toMinutes() + " minutes.");
		}
		redemption.cancel(now);
		redemption.getSubscription().giveBackOne();
		return view(redemption.getSubscription().getCustomer());
	}

	@Transactional(readOnly = true)
	public TodayView today(long staffId) {
		StaffUser staff = loadStaff(staffId);
		Instant startOfDay = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
		String shop = staff.getShop();
		if (shop == null) {
			return new TodayView(null, redemptions.countByRedeemedAtGreaterThanEqualAndCancelledAtIsNull(startOfDay),
					subscriptions.countByActivatedAtGreaterThanEqual(startOfDay));
		}
		return new TodayView(shop, redemptions.countByRedeemedAtGreaterThanEqualAndShopAndCancelledAtIsNull(startOfDay, shop),
				subscriptions.countByActivatedAtGreaterThanEqualAndShop(startOfDay, shop));
	}

	private StaffCardView view(Customer customer) {
		Instant now = Instant.now(clock);
		Optional<Subscription> current = subscriptions.findFirstByCustomerOrderByActivatedAtDesc(customer);
		Optional<Redemption> last = current.flatMap(redemptions::findFirstBySubscriptionAndCancelledAtIsNullOrderByRedeemedAtDesc);
		Long undoable = last.filter(r -> r.getRedeemedAt().isAfter(now.minus(UNDO_WINDOW))).map(Redemption::getId).orElse(null);
		return new StaffCardView(
				customer.getId(),
				customer.getFirstName(),
				PhoneNumbers.mask(customer.getPhone()),
				current.map(s -> s.statusAt(now)).orElse(SubscriptionStatus.NONE),
				current.map(Subscription::getDrinksLeft).orElse(0),
				current.map(Subscription::getDrinksTotal).orElse(plan.drinks()),
				current.map(Subscription::getActivatedAt).orElse(null),
				current.map(Subscription::getExpiresAt).orElse(null),
				last.map(Redemption::getRedeemedAt).orElse(null),
				undoable,
				customer.getCreatedAt(),
				new PlanView(plan.price(), plan.drinks(), plan.days()));
	}

	private Customer loadCustomer(long id) {
		return customers.findById(id).orElseThrow(() -> ApiException.notFound("Client introuvable."));
	}

	private StaffUser loadStaff(long id) {
		return staffUsers.findById(id).filter(StaffUser::isActive).orElseThrow(ApiException::forbidden);
	}
}
