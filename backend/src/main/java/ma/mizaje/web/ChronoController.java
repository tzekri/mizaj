package ma.mizaje.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ma.mizaje.security.Principals;
import ma.mizaje.service.ChronoService;
import ma.mizaje.web.dto.ChronoDtos.ChronoInfo;
import ma.mizaje.web.dto.ChronoDtos.ChronoSettings;
import ma.mizaje.web.dto.ChronoDtos.ChronoStats;
import ma.mizaje.web.dto.ChronoDtos.PrizeScanRequest;
import ma.mizaje.web.dto.ChronoDtos.PrizeView;
import ma.mizaje.web.dto.ChronoDtos.StartRequest;
import ma.mizaje.web.dto.ChronoDtos.StartView;
import ma.mizaje.web.dto.ChronoDtos.StopRequest;
import ma.mizaje.web.dto.ChronoDtos.StopView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Défi « 10 secondes » : jeu (public), remise du bon (barista), réglages et statistiques (admin). */
@RestController
public class ChronoController {

	private final ChronoService chrono;

	public ChronoController(ChronoService chrono) {
		this.chrono = chrono;
	}

	@GetMapping("/api/public/chrono")
	ChronoInfo info(@RequestParam(required = false) String device) {
		return chrono.info(device);
	}

	@PostMapping("/api/public/chrono/start")
	StartView start(@Valid @RequestBody StartRequest req, HttpServletRequest http) {
		return chrono.start(req.device(), http.getRemoteAddr());
	}

	@PostMapping("/api/public/chrono/stop")
	StopView stop(@Valid @RequestBody StopRequest req) {
		return chrono.stop(req.token(), req.elapsedMs());
	}

	@PostMapping("/api/staff/chrono/scan")
	PrizeView scan(@Valid @RequestBody PrizeScanRequest req) {
		return chrono.byCode(req.code());
	}

	@PostMapping("/api/staff/chrono/{code}/redeem")
	PrizeView redeem(@PathVariable String code) {
		return chrono.redeem(code, Principals.currentId());
	}

	@GetMapping("/api/admin/chrono")
	ChronoSettings settings() {
		return chrono.settings();
	}

	@PutMapping("/api/admin/chrono")
	ChronoSettings saveSettings(@Valid @RequestBody ChronoSettings req) {
		return chrono.saveSettings(req);
	}

	@GetMapping("/api/admin/chrono/stats")
	ChronoStats stats() {
		return chrono.stats();
	}
}
