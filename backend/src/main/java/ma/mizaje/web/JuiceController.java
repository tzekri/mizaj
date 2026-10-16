package ma.mizaje.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import ma.mizaje.otp.IpThrottle;
import ma.mizaje.security.Principals;
import ma.mizaje.service.JuiceService;
import ma.mizaje.web.dto.JuiceDtos.Catalog;
import ma.mizaje.domain.JuiceSize;
import ma.mizaje.web.dto.JuiceDtos.FavoriteView;
import ma.mizaje.web.dto.JuiceDtos.FormatRequest;
import ma.mizaje.web.dto.JuiceDtos.IngredientRequest;
import ma.mizaje.web.dto.JuiceDtos.IngredientView;
import ma.mizaje.web.dto.JuiceDtos.JuiceOrderView;
import ma.mizaje.web.dto.JuiceDtos.JuiceScanRequest;
import ma.mizaje.web.dto.JuiceDtos.JuiceStats;
import ma.mizaje.web.dto.JuiceDtos.RecipeRequest;
import ma.mizaje.web.dto.JuiceDtos.SizeView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Composeur de jus : catalogue et commande (public), favoris (abonné), comptoir (barista), ingrédients (admin). */
@RestController
public class JuiceController {

	/** Codes de jus créés par heure et par IP : large (Wi-Fi partagé du coffee shop), mais borné. */
	static final int ORDERS_PER_HOUR_PER_IP = 60;

	private final JuiceService juices;
	private final IpThrottle ipThrottle;

	public JuiceController(JuiceService juices, IpThrottle ipThrottle) {
		this.juices = juices;
		this.ipThrottle = ipThrottle;
	}

	// --- Public ---

	@GetMapping("/api/public/juice")
	Catalog catalog() {
		return juices.catalog();
	}

	@PostMapping("/api/public/juice/orders")
	JuiceOrderView order(@Valid @RequestBody RecipeRequest req, HttpServletRequest http) {
		throttle(http);
		return juices.order(req, null);
	}

	// --- Abonné (favoris) ---

	/** Même commande que la version publique, rattachée au compte de l'abonné. */
	@PostMapping("/api/me/juices/order")
	JuiceOrderView orderAsCustomer(@Valid @RequestBody RecipeRequest req, HttpServletRequest http) {
		throttle(http);
		return juices.order(req, Principals.currentId());
	}

	@GetMapping("/api/me/juices")
	List<FavoriteView> favorites() {
		return juices.favorites(Principals.currentId());
	}

	@PostMapping("/api/me/juices")
	FavoriteView addFavorite(@Valid @RequestBody RecipeRequest req) {
		return juices.addFavorite(Principals.currentId(), req);
	}

	@DeleteMapping("/api/me/juices/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteFavorite(@PathVariable long id) {
		juices.deleteFavorite(Principals.currentId(), id);
	}

	@PostMapping("/api/me/juices/{id}/order")
	JuiceOrderView orderFavorite(@PathVariable long id, HttpServletRequest http) {
		throttle(http);
		return juices.orderFavorite(Principals.currentId(), id);
	}

	// --- Comptoir ---

	@PostMapping("/api/staff/juice/scan")
	JuiceOrderView scan(@Valid @RequestBody JuiceScanRequest req) {
		return juices.byCode(req.code());
	}

	@PostMapping("/api/staff/juice/{code}/serve")
	JuiceOrderView serve(@PathVariable String code) {
		return juices.serve(code, Principals.currentId());
	}

	// --- Administration ---

	@GetMapping("/api/admin/juice/ingredients")
	List<IngredientView> ingredients() {
		return juices.allIngredients();
	}

	@PostMapping("/api/admin/juice/ingredients")
	IngredientView createIngredient(@Valid @RequestBody IngredientRequest req) {
		return juices.createIngredient(req);
	}

	@PutMapping("/api/admin/juice/ingredients/{code}")
	IngredientView updateIngredient(@PathVariable String code, @Valid @RequestBody IngredientRequest req) {
		return juices.updateIngredient(code, req);
	}

	@GetMapping("/api/admin/juice/formats")
	List<SizeView> formats() {
		return juices.allSizes();
	}

	@PutMapping("/api/admin/juice/formats/{size}")
	List<SizeView> updateFormat(@PathVariable JuiceSize size, @Valid @RequestBody FormatRequest req) {
		return juices.updateFormat(size, req);
	}

	@GetMapping("/api/admin/juice/stats")
	JuiceStats stats(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return juices.stats(from, to);
	}

	private void throttle(HttpServletRequest http) {
		if (!ipThrottle.tryAcquire("juice:" + http.getRemoteAddr(), ORDERS_PER_HOUR_PER_IP)) {
			throw ApiException.tooMany("Trop de jus composés depuis ce réseau. Réessayez dans un moment.");
		}
	}
}
