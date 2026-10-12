package ma.mizaj.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import ma.mizaj.config.PlanProperties;
import ma.mizaj.domain.Customer;
import ma.mizaj.domain.OtpChallenge;
import ma.mizaj.domain.Subscription;
import ma.mizaj.domain.SubscriptionStatus;
import ma.mizaj.repo.CustomerRepository;
import ma.mizaj.repo.RedemptionRepository;
import ma.mizaj.repo.SubscriptionRepository;
import ma.mizaj.web.ApiException;
import ma.mizaj.web.dto.Dtos.CardView;
import ma.mizaj.web.dto.Dtos.HistoryEntry;
import ma.mizaj.web.dto.Dtos.PlanView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compte client et carte d'abonnement (côté client). */
@Service
public class CardService {

	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
	private static final int TOKEN_LENGTH = 24;

	private final CustomerRepository customers;
	private final SubscriptionRepository subscriptions;
	private final RedemptionRepository redemptions;
	private final PlanProperties plan;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public CardService(CustomerRepository customers, SubscriptionRepository subscriptions,
			RedemptionRepository redemptions, PlanProperties plan, Clock clock) {
		this.customers = customers;
		this.subscriptions = subscriptions;
		this.redemptions = redemptions;
		this.plan = plan;
		this.clock = clock;
	}

	/** Après validation du code : retrouve le compte du numéro, ou le crée. */
	@Transactional
	public Customer findOrCreate(OtpChallenge validated) {
		Customer customer = customers.findByPhone(validated.getPhone())
				.orElseGet(() -> customers.save(new Customer(validated.getPhone(), validated.getFirstName(), newToken(),
						Instant.now(clock))));
		if (validated.getFirstName() != null && customer.getFirstName() == null) {
			customer.setFirstName(validated.getFirstName());
		}
		return customer;
	}

	@Transactional(readOnly = true)
	public CardView card(long customerId) {
		return view(load(customerId));
	}

	@Transactional
	public CardView updateName(long customerId, String firstName) {
		Customer customer = load(customerId);
		customer.setFirstName(firstName == null || firstName.isBlank() ? null : firstName.trim());
		return view(customer);
	}

	/** Nouveau QR code si l'ancien a été partagé (capture d'écran…) : l'ancien ne fonctionne plus. */
	@Transactional
	public CardView rotateQr(long customerId) {
		Customer customer = load(customerId);
		customer.setQrToken(newToken());
		return view(customer);
	}

	public PlanView plan() {
		return new PlanView(plan.price(), plan.drinks(), plan.days());
	}

	CardView view(Customer customer) {
		Optional<Subscription> current = subscriptions.findFirstByCustomerOrderByActivatedAtDesc(customer);
		Instant now = Instant.now(clock);
		var history = redemptions.findTop10BySubscriptionCustomerAndCancelledAtIsNullOrderByRedeemedAtDesc(customer).stream()
				.map(r -> new HistoryEntry(r.getRedeemedAt(), r.getShop()))
				.toList();
		return new CardView(
				customer.getFirstName(),
				PhoneNumbers.pretty(customer.getPhone()),
				customer.getQrToken(),
				current.map(s -> s.statusAt(now)).orElse(SubscriptionStatus.NONE),
				current.map(Subscription::getDrinksLeft).orElse(0),
				current.map(Subscription::getDrinksTotal).orElse(plan.drinks()),
				current.map(Subscription::getActivatedAt).orElse(null),
				current.map(Subscription::getExpiresAt).orElse(null),
				plan(),
				history);
	}

	private Customer load(long customerId) {
		return customers.findById(customerId).orElseThrow(() -> ApiException.unauthorized("Compte introuvable, reconnectez-vous."));
	}

	private String newToken() {
		String token;
		do {
			StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
			for (int i = 0; i < TOKEN_LENGTH; i++) {
				sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
			}
			token = sb.toString();
		} while (customers.existsByQrToken(token));
		return token;
	}
}
