package ma.mizaj.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import ma.mizaj.domain.Customer;
import ma.mizaj.domain.OtpChallenge;
import ma.mizaj.domain.Plan;
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
	private final PlanService plans;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public CardService(CustomerRepository customers, SubscriptionRepository subscriptions,
			RedemptionRepository redemptions, PlanService plans, Clock clock) {
		this.customers = customers;
		this.subscriptions = subscriptions;
		this.redemptions = redemptions;
		this.plans = plans;
		this.clock = clock;
	}

	/** Après validation du code : retrouve le compte du numéro, ou le crée, et enregistre la formule choisie. */
	@Transactional
	public Customer findOrCreate(OtpChallenge validated) {
		Customer customer = customers.findByPhone(validated.getPhone())
				.orElseGet(() -> customers.save(new Customer(validated.getPhone(), validated.getFirstName(), newToken(),
						Instant.now(clock))));
		if (validated.getFirstName() != null && customer.getFirstName() == null) {
			customer.setFirstName(validated.getFirstName());
		}
		if (validated.getPlanCode() != null) {
			try {
				customer.setPreferredPlan(plans.activeByCode(validated.getPlanCode()));
			} catch (ApiException e) {
				// Formule désactivée entre l'envoi et la validation du code : on garde la précédente.
			}
		}
		return customer;
	}

	/**
	 * Inscription directe, sans code : uniquement pour un numéro inconnu.
	 * Un numéro qui a déjà une carte ne s'ouvre pas ici (sinon n'importe qui pourrait prendre la carte d'un autre) :
	 * le client la récupère avec un code donné en caisse.
	 */
	@Transactional
	public Customer signup(String rawPhone, String firstName, String planCode) {
		String phone = PhoneNumbers.normalize(rawPhone);
		Plan plan = plans.activeByCode(planCode);
		if (customers.findByPhone(phone).isPresent()) {
			throw ApiException.conflict("EXISTS", "Ce numéro a déjà une carte Mizaj. Pour l'ouvrir sur ce téléphone, "
					+ "demandez votre code de récupération au barista, puis saisissez-le ci-dessous.");
		}
		Customer customer = customers.save(new Customer(phone, firstName == null || firstName.isBlank() ? null : firstName.trim(),
				newToken(), Instant.now(clock)));
		customer.setPreferredPlan(plan);
		return customer;
	}

	@Transactional(readOnly = true)
	public CardView card(long customerId) {
		return view(load(customerId));
	}

	@Transactional
	public CardView updateProfile(long customerId, String firstName, String planCode) {
		Customer customer = load(customerId);
		if (firstName != null) {
			customer.setFirstName(firstName.isBlank() ? null : firstName.trim());
		}
		Plan plan = plans.activeByCode(planCode);
		if (plan != null) {
			customer.setPreferredPlan(plan);
		}
		return view(customer);
	}

	/** Nouveau QR code si l'ancien a été partagé (capture d'écran…) : l'ancien ne fonctionne plus. */
	@Transactional
	public CardView rotateQr(long customerId) {
		Customer customer = load(customerId);
		customer.setQrToken(newToken());
		return view(customer);
	}

	CardView view(Customer customer) {
		Subscription current = subscriptions.findFirstByCustomerOrderByActivatedAtDesc(customer).orElse(null);
		Instant now = Instant.now(clock);
		var history = redemptions.findTop10BySubscriptionCustomerAndCancelledAtIsNullOrderByRedeemedAtDesc(customer).stream()
				.map(r -> new HistoryEntry(r.getRedeemedAt(), r.getShop()))
				.toList();
		Plan plan = plans.displayed(current, customer);
		return new CardView(
				customer.getFirstName(),
				PhoneNumbers.pretty(customer.getPhone()),
				customer.getQrToken(),
				current == null ? SubscriptionStatus.NONE : current.statusAt(now),
				current == null ? 0 : current.getDrinksLeft(),
				current == null ? plan.getDrinks() : current.getDrinksTotal(),
				current == null ? null : current.getActivatedAt(),
				current == null ? null : current.getExpiresAt(),
				PlanView.of(plan),
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
