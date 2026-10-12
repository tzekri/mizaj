package ma.mizaj.web;

import jakarta.validation.Valid;
import ma.mizaj.security.Principals;
import ma.mizaj.service.CardService;
import ma.mizaj.web.dto.Dtos.CardView;
import ma.mizaj.web.dto.Dtos.UpdateProfileRequest;
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
		return cards.updateName(Principals.currentId(), req.firstName());
	}

	@PostMapping("/qr/rotate")
	CardView rotate() {
		return cards.rotateQr(Principals.currentId());
	}
}
