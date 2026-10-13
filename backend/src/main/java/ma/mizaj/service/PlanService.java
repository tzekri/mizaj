package ma.mizaj.service;

import java.text.Normalizer;
import java.util.List;
import ma.mizaj.domain.Customer;
import ma.mizaj.domain.Plan;
import ma.mizaj.domain.Subscription;
import ma.mizaj.repo.PlanRepository;
import ma.mizaj.web.ApiException;
import ma.mizaj.web.dto.Dtos.PlanRequest;
import ma.mizaj.web.dto.Dtos.PlanView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Formules d'abonnement : catalogue public et gestion par l'administrateur. */
@Service
public class PlanService {

	private final PlanRepository plans;

	public PlanService(PlanRepository plans) {
		this.plans = plans;
	}

	@Transactional(readOnly = true)
	public List<PlanView> activePlans() {
		return plans.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(PlanView::of).toList();
	}

	@Transactional(readOnly = true)
	public List<PlanView> allPlans() {
		return plans.findAllByOrderBySortOrderAscIdAsc().stream().map(PlanView::of).toList();
	}

	/** Formule proposable à un client (active). Code vide → null. */
	public Plan activeByCode(String code) {
		if (code == null || code.isBlank()) {
			return null;
		}
		return plans.findByCodeAndActiveTrue(code.trim())
				.orElseThrow(() -> ApiException.badRequest("Cette formule n'est plus proposée. Choisissez-en une autre."));
	}

	/** Formule par défaut quand le client n'en a choisi aucune : la première du catalogue. */
	public Plan defaultPlan() {
		return plans.findByActiveTrueOrderBySortOrderAscIdAsc().stream().findFirst()
				.orElseThrow(() -> ApiException.conflict("NO_PLAN", "Aucune formule d'abonnement n'est active."));
	}

	/** Formule à afficher : celle de la période en cours, sinon celle choisie par le client, sinon la formule par défaut. */
	public Plan displayed(Subscription current, Customer customer) {
		if (current != null && current.getPlan() != null) {
			return current.getPlan();
		}
		return proposed(customer);
	}

	/** Formule à proposer au barista pour une (ré)activation. */
	public Plan proposed(Customer customer) {
		Plan preferred = customer.getPreferredPlan();
		return preferred != null && preferred.isActive() ? preferred : defaultPlan();
	}

	@Transactional
	public PlanView create(PlanRequest req) {
		String code = slug(req.name());
		String unique = code;
		for (int i = 2; plans.existsByCode(unique); i++) {
			unique = code + "-" + i;
		}
		Plan plan = new Plan(unique, req.name().trim(), blankToNull(req.description()), req.price(), req.drinks(), req.days(),
				req.unit().trim(), req.unitPlural().trim(), (int) plans.count() + 1);
		plan.setActive(req.active());
		return PlanView.of(plans.save(plan));
	}

	@Transactional
	public PlanView update(String code, PlanRequest req) {
		Plan plan = plans.findByCode(code).orElseThrow(() -> ApiException.notFound("Formule introuvable."));
		plan.update(req.name().trim(), blankToNull(req.description()), req.price(), req.drinks(), req.days(),
				req.unit().trim(), req.unitPlural().trim());
		plan.setActive(req.active());
		return PlanView.of(plan);
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	static String slug(String name) {
		String s = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return s.isEmpty() ? "formule" : s.substring(0, Math.min(s.length(), 36));
	}
}
