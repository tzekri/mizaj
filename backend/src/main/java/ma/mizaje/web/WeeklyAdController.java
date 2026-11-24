package ma.mizaje.web;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import ma.mizaje.service.WeeklyAdService;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdImage;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdPublic;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdRequest;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdView;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Pub de la semaine : affichage (public) et programmation (admin). */
@RestController
public class WeeklyAdController {

	private final WeeklyAdService ads;

	public WeeklyAdController(WeeklyAdService ads) {
		this.ads = ads;
	}

	/** Pub du jour ; 204 si aucune n'est programmée. */
	@GetMapping("/api/public/weekly-ad")
	ResponseEntity<WeeklyAdPublic> current() {
		return ads.current().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
	}

	/** Affiche ; l'adresse change avec l'image (?v=…), elle peut donc rester en cache longtemps. */
	@GetMapping("/api/public/weekly-ad/{id}/image")
	ResponseEntity<byte[]> image(@PathVariable long id) {
		WeeklyAdImage img = ads.image(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(img.type()))
				.cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
				.body(img.data());
	}

	// --- Administration ---

	@GetMapping("/api/admin/weekly-ads")
	List<WeeklyAdView> list() {
		return ads.list();
	}

	@PostMapping("/api/admin/weekly-ads")
	WeeklyAdView create(@Valid @RequestBody WeeklyAdRequest req) {
		return ads.create(req);
	}

	@PutMapping("/api/admin/weekly-ads/{id}")
	WeeklyAdView update(@PathVariable long id, @Valid @RequestBody WeeklyAdRequest req) {
		return ads.update(id, req);
	}

	@DeleteMapping("/api/admin/weekly-ads/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		ads.delete(id);
	}
}
