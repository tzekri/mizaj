package ma.mizaje.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import ma.mizaje.domain.Customer;
import ma.mizaje.domain.Plan;
import ma.mizaje.domain.Redemption;
import ma.mizaje.domain.StaffUser;
import ma.mizaje.domain.Subscription;
import ma.mizaje.domain.SubscriptionStatus;
import ma.mizaje.repo.CustomerRepository;
import ma.mizaje.repo.RedemptionRepository;
import ma.mizaje.repo.StaffUserRepository;
import ma.mizaje.repo.SubscriptionRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.Dtos.PlanView;
import ma.mizaje.web.dto.Dtos.StaffCardView;
import ma.mizaje.web.dto.Dtos.TodayView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opérations du barista au comptoir : lecture de la carte, validation d'une boisson, activation après paiement. */
@Service
public class CounterService {

	/** En dessous de ce délai depuis la dernière boisson, une confirmation est demandée (double scan). */
	static final Duration DOUBLE_SCAN_WINDOW = Duration.ofMinutes(2);
	/** Une validation peut être annulée pendant ce délai (erreur de manipulation). */
	static final Duration UNDO_WINDOW = Duration.ofMinutes(10);
	static final String QR_PREFIX = "MIZAJE:";
	/** Préfixe des cartes générées des premières versions (ancien nom « Mizaj »), toujours accepté. */
	static final String LEGACY_QR_PREFIX = "MIZAJ:";

	private final CustomerRepository customers;
	private final SubscriptionRepository subscriptions;
	private final RedemptionRepository redemptions;
	private final StaffUserRepository staffUsers;
	private final PlanService plans;
	private final Clock clock;
	private final ZoneId zone;

	public CounterService(CustomerRepository customers, SubscriptionRepository subscriptions,
			RedemptionRepository redemptions, StaffUserRepository staffUsers, PlanService plans, Clock clock, ZoneId appZone) {
		this.customers = customers;
		this.subscriptions = subscriptions;
		this.redemptions = redemptions;
		this.staffUsers = staffUsers;
		this.plans = plans;
		this.clock = clock;
		this.zone = appZone;
	}

	@Transactional(readOnly = true)
	public StaffCardView byToken(String scanned) {
		String token = scanned.trim();
		if (token.startsWith(QR_PREFIX)) {
			token = token.substring(QR_PREFIX.length());
		} else if (token.startsWith(LEGACY_QR_PREFIX)) {
			token = token.substring(LEGACY_QR_PREFIX.length());
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
				.orElseThrow(() -> ApiException.conflict("NO_SUBSCRIPTION", "Ce client n'a pas encore d'abonnement actif. Encaissez la formule puis activez-la."));
		String unitPlural = sub.getPlan() == null ? "boissons" : sub.getPlan().getUnitPlural();
		switch (sub.statusAt(now)) {
			case EXPIRED -> throw ApiException.conflict("EXPIRED", "Abonnement expiré. Proposez au client de le renouveler.");
			case EMPTY -> throw ApiException.conflict("EMPTY", "Toutes les boissons de cet abonnement (" + sub.getDrinksTotal() + " " + unitPlural + ") ont été servies. Proposez le renouvellement.");
			default -> {
			}
		}
		if (!force) {
			redemptions.findFirstBySubscriptionAndCancelledAtIsNullOrderByRedeemedAtDesc(sub)
					.filter(r -> r.getRedeemedAt().isAfter(now.minus(DOUBLE_SCAN_WINDOW)))
					.ifPresent(r -> {
						long seconds = Duration.between(r.getRedeemedAt(), now).toSeconds();
						throw ApiException.conflict("RECENT", "Une boisson a déjà été validée sur cette carte il y a " + seconds + " s. Valider quand même ?");
					});
		}
		sub.consumeOne();
		redemptions.save(new Redemption(sub, loadStaff(staffId), now));
		return view(customer);
	}

	/**
	 * Le client a payé en caisse : nouvelle période selon la formule choisie
	 * (celle demandée par le barista, sinon celle choisie par le client à l'inscription).
	 */
	@Transactional
	public StaffCardView activate(long customerId, long staffId, String planCode, boolean confirmReplace) {
		Customer customer = loadCustomer(customerId);
		Plan plan = Optional.ofNullable(plans.activeByCode(planCode)).orElseGet(() -> plans.proposed(customer));
		Instant now = Instant.now(clock);
		Optional<Subscription> current = subscriptions.findFirstByCustomerOrderByActivatedAtDesc(customer);
		if (current.isPresent() && current.get().statusAt(now) == SubscriptionStatus.ACTIVE) {
			if (!confirmReplace) {
				throw ApiException.conflict("STILL_ACTIVE", "L'abonnement en cours a encore " + current.get().getDrinksLeft()
						+ " boisson(s). Elles seront perdues (pas de report). Renouveler quand même ?");
			}
			current.get().closeAt(now);
		}
		customer.setPreferredPlan(plan);
		subscriptions.save(new Subscription(customer, plan, now, loadStaff(staffId)));
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
		Subscription current = subscriptions.findFirstByCustomerOrderByActivatedAtDesc(customer).orElse(null);
		Optional<Redemption> last = Optional.ofNullable(current).flatMap(redemptions::findFirstBySubscriptionAndCancelledAtIsNullOrderByRedeemedAtDesc);
		Long undoable = last.filter(r -> r.getRedeemedAt().isAfter(now.minus(UNDO_WINDOW))).map(Redemption::getId).orElse(null);
		Plan displayed = plans.displayed(current, customer);
		return new StaffCardView(
				customer.getId(),
				customer.getFirstName(),
				PhoneNumbers.mask(customer.getPhone()),
				current == null ? SubscriptionStatus.NONE : current.statusAt(now),
				current == null ? 0 : current.getDrinksLeft(),
				current == null ? displayed.getDrinks() : current.getDrinksTotal(),
				current == null ? null : current.getActivatedAt(),
				current == null ? null : current.getExpiresAt(),
				last.map(Redemption::getRedeemedAt).orElse(null),
				undoable,
				customer.getCreatedAt(),
				PlanView.of(displayed),
				PlanView.of(plans.proposed(customer)));
	}

	private Customer loadCustomer(long id) {
		return customers.findById(id).orElseThrow(() -> ApiException.notFound("Client introuvable."));
	}

	private StaffUser loadStaff(long id) {
		return staffUsers.findById(id).filter(StaffUser::isActive).orElseThrow(ApiException::forbidden);
	}
}
