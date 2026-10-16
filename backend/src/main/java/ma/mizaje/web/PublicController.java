package ma.mizaje.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import ma.mizaje.domain.Customer;
import ma.mizaje.otp.IpThrottle;
import ma.mizaje.otp.OtpService;
import ma.mizaje.security.TokenService;
import ma.mizaje.service.CardService;
import ma.mizaje.service.PlanService;
import ma.mizaje.web.dto.Dtos.CustomerSession;
import ma.mizaje.web.dto.Dtos.OtpStartRequest;
import ma.mizaje.web.dto.Dtos.OtpStartResponse;
import ma.mizaje.web.dto.Dtos.OtpVerifyRequest;
import ma.mizaje.web.dto.Dtos.PlanView;
import ma.mizaje.web.dto.Dtos.SignupConfig;
import ma.mizaje.web.dto.Dtos.SignupRequest;
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
	private final IpThrottle ipThrottle;

	/** Inscriptions directes par heure et par IP : large, les clients d'un coffee shop partagent le même Wi-Fi. */
	static final int SIGNUPS_PER_HOUR_PER_IP = 40;

	public PublicController(OtpService otp, CardService cards, TokenService tokens, PlanService plans, IpThrottle ipThrottle) {
		this.otp = otp;
		this.cards = cards;
		this.tokens = tokens;
		this.plans = plans;
		this.ipThrottle = ipThrottle;
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

	@GetMapping("/config")
	SignupConfig config() {
		return new SignupConfig(otp.verificationEnabled());
	}

	/** Inscription directe (sans code) : disponible seulement quand l'envoi de code n'est pas configuré. */
	@PostMapping("/signup")
	CustomerSession signup(@Valid @RequestBody SignupRequest req, HttpServletRequest http) {
		if (otp.verificationEnabled()) {
			throw ApiException.badRequest("Un code de validation est requis : utilisez « Recevoir mon code ».");
		}
		if (!ipThrottle.tryAcquire("signup:" + http.getRemoteAddr(), SIGNUPS_PER_HOUR_PER_IP)) {
			throw ApiException.tooMany("Trop d'inscriptions depuis ce réseau. Réessayez dans un moment.");
		}
		Customer customer = cards.signup(req.phone(), req.firstName(), req.planCode());
		return new CustomerSession(tokens.issue(customer), cards.card(customer.getId()));
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
