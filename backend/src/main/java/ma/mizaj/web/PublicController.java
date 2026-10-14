package ma.mizaj.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import ma.mizaj.domain.Customer;
import ma.mizaj.otp.OtpService;
import ma.mizaj.security.TokenService;
import ma.mizaj.service.CardService;
import ma.mizaj.service.PlanService;
import ma.mizaj.web.dto.Dtos.CustomerSession;
import ma.mizaj.web.dto.Dtos.OtpStartRequest;
import ma.mizaj.web.dto.Dtos.OtpStartResponse;
import ma.mizaj.web.dto.Dtos.OtpVerifyRequest;
import ma.mizaj.web.dto.Dtos.PlanView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inscription / connexion des clients par code WhatsApp (sans mot de passe). */
@RestController
@RequestMapping("/api/public")
public class PublicController {

	private final OtpService otp;
	private final CardService cards;
	private final TokenService tokens;
	private final PlanService plans;

	public PublicController(OtpService otp, CardService cards, TokenService tokens, PlanService plans) {
		this.otp = otp;
		this.cards = cards;
		this.tokens = tokens;
		this.plans = plans;
	}

	@GetMapping("/health")
	Map<String, String> health() {
		return Map.of("status", "ok");
	}

	/** Formules proposées sur la page Abonnement. */
	@GetMapping("/plans")
	List<PlanView> plans() {
		return plans.activePlans();
	}

	@PostMapping("/otp/start")
	OtpStartResponse start(@Valid @RequestBody OtpStartRequest req, HttpServletRequest http) {
		return otp.start(req.phone(), req.firstName(), req.planCode(), req.channel(), http.getRemoteAddr());
	}

	@PostMapping("/otp/verify")
	CustomerSession verify(@Valid @RequestBody OtpVerifyRequest req) {
		Customer customer = cards.findOrCreate(otp.verify(req.phone(), req.code()));
		return new CustomerSession(tokens.issue(customer), cards.card(customer.getId()));
	}
}
