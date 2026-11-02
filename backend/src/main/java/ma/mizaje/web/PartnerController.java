package ma.mizaje.web;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import ma.mizaje.service.PartnerService;
import ma.mizaje.web.dto.MenuDtos.MoveRequest;
import ma.mizaje.web.dto.PartnerDtos.AdView;
import ma.mizaje.web.dto.PartnerDtos.PartnerImage;
import ma.mizaje.web.dto.PartnerDtos.PartnerRequest;
import ma.mizaje.web.dto.PartnerDtos.PartnerView;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Pubs des partenaires : diffusion sur l'écran géant (public) et gestion (admin). */
@RestController
public class PartnerController {

	private final PartnerService partners;

	public PartnerController(PartnerService partners) {
		this.partners = partners;
	}

	@GetMapping("/api/public/partners/ads")
	List<AdView> ads(@RequestParam(required = false) String shop) {
		return partners.ads(blankToNull(shop));
	}

	@PostMapping("/api/public/partners/{id}/shown")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void shown(@PathVariable long id, @RequestParam(required = false) String shop, @RequestParam(defaultValue = "-") String screen) {
		partners.shown(id, blankToNull(shop), screen.length() > 16 ? screen.substring(0, 16) : screen);
	}

	/** Image d'une pub ; l'adresse change avec l'image (?v=…), elle peut donc rester en cache longtemps. */
	@GetMapping("/api/public/partners/{id}/image")
	ResponseEntity<byte[]> image(@PathVariable long id) {
		PartnerImage img = partners.image(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(img.type()))
				.cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
				.body(img.data());
	}

	// --- Administration ---

	@GetMapping("/api/admin/partners")
	List<PartnerView> list() {
		return partners.list();
	}

	@PostMapping("/api/admin/partners")
	PartnerView create(@Valid @RequestBody PartnerRequest req) {
		return partners.create(req);
	}

	@PutMapping("/api/admin/partners/{id}")
	PartnerView update(@PathVariable long id, @Valid @RequestBody PartnerRequest req) {
		return partners.update(id, req);
	}

	@PostMapping("/api/admin/partners/{id}/move")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void move(@PathVariable long id, @Valid @RequestBody MoveRequest req) {
		partners.move(id, "up".equals(req.direction()));
	}

	@DeleteMapping("/api/admin/partners/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		partners.delete(id);
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
