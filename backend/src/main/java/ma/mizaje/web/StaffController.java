package ma.mizaje.web;

import jakarta.validation.Valid;
import ma.mizaje.domain.StaffUser;
import ma.mizaje.repo.StaffUserRepository;
import ma.mizaje.security.Principals;
import ma.mizaje.security.TokenService;
import ma.mizaje.service.CounterService;
import ma.mizaje.web.dto.Dtos.ActivateRequest;
import ma.mizaje.otp.OtpService;
import ma.mizaje.repo.CustomerRepository;
import ma.mizaje.web.dto.Dtos.RecoveryCodeView;
import ma.mizaje.web.dto.Dtos.RedeemRequest;
import ma.mizaje.web.dto.Dtos.ScanRequest;
import ma.mizaje.web.dto.Dtos.StaffCardView;
import ma.mizaje.web.dto.Dtos.StaffLoginRequest;
import ma.mizaje.web.dto.Dtos.StaffSession;
import ma.mizaje.web.dto.Dtos.TodayView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Espace barista : connexion, scan de la carte, validation d'un espresso, activation d'un abonnement. */
@RestController
public class StaffController {

	private final CounterService counter;
	private final StaffUserRepository staffUsers;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokens;
	private final OtpService otp;
	private final CustomerRepository customers;

	public StaffController(CounterService counter, StaffUserRepository staffUsers, PasswordEncoder passwordEncoder,
			TokenService tokens, OtpService otp, CustomerRepository customers) {
		this.counter = counter;
		this.staffUsers = staffUsers;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.otp = otp;
		this.customers = customers;
	}

	@PostMapping("/api/auth/staff/login")
	StaffSession login(@Valid @RequestBody StaffLoginRequest req) {
		StaffUser user = staffUsers.findByEmailIgnoreCase(req.email().trim())
				.filter(StaffUser::isActive)
				.filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
				.orElseThrow(() -> ApiException.unauthorized("E-mail ou mot de passe incorrect."));
		return new StaffSession(tokens.issue(user), user.getFullName(), user.getRole(), user.getShop());
	}

	/** Le contenu du QR code est envoyé dans le corps (pas dans l'URL, pour ne pas finir dans les logs). */
	@PostMapping("/api/staff/scan")
	StaffCardView scan(@Valid @RequestBody ScanRequest req) {
		return counter.byToken(req.token());
	}

	@GetMapping("/api/staff/lookup")
	StaffCardView lookup(@RequestParam String phone) {
		return counter.byPhone(phone);
	}

	@PostMapping("/api/staff/customers/{id}/redeem")
	StaffCardView redeem(@PathVariable long id, @RequestBody(required = false) RedeemRequest req) {
		return counter.redeem(id, Principals.currentId(), req != null && req.force());
	}

	@PostMapping("/api/staff/customers/{id}/activate")
	StaffCardView activate(@PathVariable long id, @RequestBody(required = false) ActivateRequest req) {
		return counter.activate(id, Principals.currentId(), req == null ? null : req.planCode(), req != null && req.confirmReplace());
	}

	/** Le barista lit ce code au client présent en caisse, qui le saisit pour ouvrir sa carte sur son téléphone. */
	@PostMapping("/api/staff/customers/{id}/recovery-code")
	RecoveryCodeView recoveryCode(@PathVariable long id) {
		var customer = customers.findById(id).orElseThrow(() -> ApiException.notFound("Client introuvable."));
		var code = otp.issueRecoveryCode(customer.getPhone());
		return new RecoveryCodeView(code.code(), code.expiresAt());
	}

	@PostMapping("/api/staff/redemptions/{id}/undo")
	StaffCardView undo(@PathVariable long id) {
		return counter.undo(id);
	}

	@GetMapping("/api/staff/today")
	TodayView today() {
		return counter.today(Principals.currentId());
	}
}
