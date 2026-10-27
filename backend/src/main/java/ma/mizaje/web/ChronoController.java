package ma.mizaje.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ma.mizaje.security.Principals;
import java.util.Map;
import ma.mizaje.service.ChronoScreenHub;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Défi « 10 secondes » : jeu (public), remise du bon (barista), réglages et statistiques (admin). */
@RestController
public class ChronoController {

	private final ChronoService chrono;
	private final ChronoScreenHub screens;

	public ChronoController(ChronoService chrono, ChronoScreenHub screens) {
		this.chrono = chrono;
		this.screens = screens;
	}

	/** Heure du serveur (ms) : la TV et les téléphones calent leur horloge et mesurent le délai réseau. */
	@GetMapping("/api/public/chrono/ping")
	Map<String, Long> ping() {
		return Map.of("serverTime", System.currentTimeMillis());
	}

	/** État en direct d'un écran géant (TV et téléphones manettes). */
	@GetMapping(path = "/api/public/chrono/screens/{screen:[A-Za-z0-9]{6,16}}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter screenEvents(@PathVariable String screen) {
		return screens.subscribe(screen);
	}

	@GetMapping("/api/public/chrono")
	ChronoInfo info(@RequestParam(required = false) String device) {
		return chrono.info(device);
	}

	@PostMapping("/api/public/chrono/start")
	StartView start(@Valid @RequestBody StartRequest req, HttpServletRequest http) {
		return chrono.start(req.device(), http.getRemoteAddr(), req.screen());
	}

	@PostMapping("/api/public/chrono/stop")
	StopView stop(@Valid @RequestBody StopRequest req) {
		return chrono.stop(req.token(), req.elapsedMs(), req.latencyMs());
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
