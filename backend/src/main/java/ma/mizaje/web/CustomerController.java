package ma.mizaje.web;

import jakarta.validation.Valid;
import ma.mizaje.security.Principals;
import ma.mizaje.service.CardService;
import ma.mizaje.web.dto.Dtos.CardView;
import ma.mizaje.web.dto.Dtos.UpdateProfileRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Carte du client connecté. */
@RestController
@RequestMapping("/api/me")
public class CustomerController {

	private final CardService cards;

	public CustomerController(CardService cards) {
		this.cards = cards;
	}

	@GetMapping
	CardView card() {
		return cards.card(Principals.currentId());
	}

	@PatchMapping
	CardView update(@Valid @RequestBody UpdateProfileRequest req) {
		return cards.updateProfile(Principals.currentId(), req.firstName(), req.planCode());
	}

	@PostMapping("/qr/rotate")
	CardView rotate() {
		return cards.rotateQr(Principals.currentId());
	}
}
