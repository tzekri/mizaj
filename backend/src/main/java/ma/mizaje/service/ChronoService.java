package ma.mizaje.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import ma.mizaje.domain.AppSetting;
import ma.mizaje.domain.ChronoAttempt;
import ma.mizaje.domain.ChronoPrize;
import ma.mizaje.domain.StaffUser;
import ma.mizaje.repo.AppSettingRepository;
import ma.mizaje.repo.ChronoAttemptRepository;
import ma.mizaje.repo.ChronoPrizeRepository;
import ma.mizaje.repo.StaffUserRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.ChronoDtos.ChronoInfo;
import ma.mizaje.web.dto.ChronoDtos.ChronoSettings;
import ma.mizaje.web.dto.ChronoDtos.ChronoStats;
import ma.mizaje.web.dto.ChronoDtos.PrizeView;
import ma.mizaje.web.dto.ChronoDtos.StartView;
import ma.mizaje.web.dto.ChronoDtos.StopView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Défi « 10 secondes » : le joueur démarre le chrono et l'arrête ; s'il s'arrête pile sur 10.00 s
 * (de 10,000 à 10,009 s), il gagne un bon cadeau à montrer en caisse.
 * Le départ est enregistré par le serveur : un temps annoncé plus long que le temps réellement écoulé est refusé.
 */
@Service
public class ChronoService {

	public static final String QR_PREFIX = "KIDIMA-GAIN:";
	static final int TARGET_MS = 10_000;
	/** 10.00 s affiché (centièmes tronqués) : de 10 000 à 10 009 ms. */
	static final int WIN_WINDOW_MS = 10;
	/** Essais par jour et par appareil par défaut ; réglable dans l'administration (0 = illimité, pour les tests). */
	static final int DEFAULT_ATTEMPTS_PER_DAY = 3;
	/** Plafond par adresse IP (Wi-Fi partagé du coffee shop) pour limiter les abus. */
	static final int ATTEMPTS_PER_IP_PER_DAY = 80;
	/** Tolérance d'horloge : le téléphone ne peut pas annoncer plus que le temps écoulé côté serveur. */
	static final long CLOCK_TOLERANCE_MS = 300;
	/** Au-delà, l'essai a trop traîné sur le réseau pour être fiable. */
	static final long MAX_NETWORK_LAG_MS = 15_000;
	/** Délai réseau du téléphone manette pris en compte au plus (moitié de l'aller-retour mesuré). */
	static final int MAX_LATENCY_MS = 250;

	static final String KEY_ENABLED = "chrono.enabled";
	static final String KEY_PRIZE = "chrono.prize";
	static final String KEY_VALIDITY = "chrono.validity-days";
	static final String KEY_MAX_WINNERS = "chrono.max-winners-per-day";
	static final String KEY_ATTEMPTS = "chrono.attempts-per-day";
	static final String DEFAULT_PRIZE = "Un café offert";
	static final int DEFAULT_VALIDITY_DAYS = 7;
	static final int DEFAULT_MAX_WINNERS = 20;

	private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

	private final ChronoAttemptRepository attempts;
	private final ChronoPrizeRepository prizes;
	private final AppSettingRepository settings;
	private final StaffUserRepository staffUsers;
	private final ChronoScreenHub screens;
	private final Clock clock;
	private final ZoneId zone;
	private final SecureRandom random = new SecureRandom();

	public ChronoService(ChronoAttemptRepository attempts, ChronoPrizeRepository prizes, AppSettingRepository settings,
			StaffUserRepository staffUsers, ChronoScreenHub screens, Clock clock, ZoneId appZone) {
		this.attempts = attempts;
		this.prizes = prizes;
		this.settings = settings;
		this.staffUsers = staffUsers;
		this.screens = screens;
		this.clock = clock;
		this.zone = appZone;
	}

	// --- Joueur ---

	@Transactional(readOnly = true)
	public ChronoInfo info(String device) {
		ChronoSettings s = settings();
		int used = device == null ? 0 : (int) attempts.countByDeviceAndStartedAtGreaterThanEqual(device, startOfToday());
		return new ChronoInfo(s.enabled(), s.prize(), s.attemptsPerDay(), remaining(s.attemptsPerDay(), used), s.validityDays());
	}

	@Transactional
	public StartView start(String device, String ip) {
		return start(device, ip, null);
	}

	/** screen : écran géant sur lequel s'affiche la partie (le téléphone sert de manette) ; null : téléphone seul. */
	@Transactional
	public StartView start(String device, String ip, String screen) {
		if (!settings().enabled()) {
			throw ApiException.conflict("GAME_OFF", "Le défi est en pause pour le moment. Revenez bientôt !");
		}
		Instant today = startOfToday();
		int max = settings().attemptsPerDay();
		long used = attempts.countByDeviceAndStartedAtGreaterThanEqual(device, today);
		if (max > 0 && used >= max) {
			throw ApiException.tooMany("Vous avez utilisé vos " + max + " essais du jour. Revenez demain !");
		}
		if (max > 0 && attempts.countByIpAndStartedAtGreaterThanEqual(ip, today) >= ATTEMPTS_PER_IP_PER_DAY) {
			throw ApiException.tooMany("Trop d'essais depuis ce réseau aujourd'hui. Revenez demain !");
		}
		if (screen != null && screens.busy(screen)) {
			throw ApiException.conflict("SCREEN_BUSY", "Un joueur est en train de jouer sur l'écran. Patientez quelques secondes !");
		}
		Instant now = Instant.now(clock);
		ChronoAttempt attempt = attempts.save(new ChronoAttempt(token(), device, ip, now, screen));
		if (screen != null) {
			screens.running(screen, attempt.getToken(), now.toEpochMilli());
		}
		return new StartView(attempt.getToken(), remaining(max, (int) used + 1));
	}

	@Transactional
	public StopView stop(String token, int elapsedMs) {
		return stop(token, elapsedMs, null);
	}

	/**
	 * Sur écran géant, le joueur regarde le chrono de la TV (calé sur l'heure du serveur) : le temps est mesuré par le
	 * serveur, de la réception de « Démarrer » à celle de « Arrêter », moins le trajet réseau de « Arrêter »
	 * (latencyMs, moitié de l'aller-retour mesuré par le téléphone, plafonnée).
	 */
	@Transactional
	public StopView stop(String token, int elapsedMs, Integer latencyMs) {
		ChronoAttempt attempt = attempts.findByToken(token).orElseThrow(() -> ApiException.notFound("Essai introuvable."));
		if (attempt.getStoppedAt() != null) {
			throw ApiException.conflict("ALREADY_STOPPED", "Cet essai est déjà terminé.");
		}
		Instant now = Instant.now(clock);
		long serverMs = Duration.between(attempt.getStartedAt(), now).toMillis();
		int remaining = remaining(settings().attemptsPerDay(),
				(int) attempts.countByDeviceAndStartedAtGreaterThanEqual(attempt.getDevice(), startOfToday()));
		if (attempt.getScreen() != null) {
			int latency = latencyMs == null ? 0 : Math.max(0, Math.min(MAX_LATENCY_MS, latencyMs));
			elapsedMs = (int) Math.max(0, serverMs - latency);
		} else if (elapsedMs > serverMs + CLOCK_TOLERANCE_MS || serverMs - elapsedMs > MAX_NETWORK_LAG_MS) {
			attempt.stop(now, null, false);
			throw ApiException.badRequest("Essai non valable : la connexion a été trop lente. Réessayez avec une bonne connexion.");
		}
		boolean win = isWin(elapsedMs) && prizes.countByCreatedAtGreaterThanEqual(startOfToday()) < settings().maxWinnersPerDay();
		attempt.stop(now, elapsedMs, win);
		PrizeView prize = null;
		if (win) {
			ChronoSettings s = settings();
			prize = view(prizes.save(new ChronoPrize(newCode(), s.prize(), elapsedMs, now, now.plus(Duration.ofDays(s.validityDays())))));
		}
		if (attempt.getScreen() != null) {
			screens.result(attempt.getScreen(), token, elapsedMs, win, win ? prize.label() : null);
		}
		return new StopView(elapsedMs, win, prize, remaining);
	}

	/** Essais restants aujourd'hui ; 0 si les essais sont illimités (max = 0). */
	private static int remaining(int max, int used) {
		return max == 0 ? 0 : Math.max(0, max - used);
	}

	static boolean isWin(int elapsedMs) {
		return elapsedMs >= TARGET_MS && elapsedMs < TARGET_MS + WIN_WINDOW_MS;
	}

	// --- Comptoir ---

	@Transactional(readOnly = true)
	public PrizeView byCode(String raw) {
		return view(load(raw));
	}

	/** Le barista remet le cadeau. Un bon ne sert qu'une fois. */
	@Transactional
	public PrizeView redeem(String raw, long staffId) {
		ChronoPrize prize = load(raw);
		Instant now = Instant.now(clock);
		if (prize.getRedeemedAt() != null) {
			throw ApiException.conflict("ALREADY_REDEEMED", "Ce bon a déjà été utilisé.");
		}
		if (!prize.getExpiresAt().isAfter(now)) {
			throw ApiException.conflict("EXPIRED", "Ce bon a expiré.");
		}
		StaffUser staff = staffUsers.findById(staffId).filter(StaffUser::isActive).orElseThrow(ApiException::forbidden);
		prize.redeem(staff, now);
		return view(prize);
	}

	// --- Administration ---

	@Transactional(readOnly = true)
	public ChronoSettings settings() {
		return new ChronoSettings(
				!"false".equals(get(KEY_ENABLED, "true")),
				get(KEY_PRIZE, DEFAULT_PRIZE),
				intValue(get(KEY_VALIDITY, null), DEFAULT_VALIDITY_DAYS),
				intValue(get(KEY_MAX_WINNERS, null), DEFAULT_MAX_WINNERS),
				intValue(get(KEY_ATTEMPTS, null), DEFAULT_ATTEMPTS_PER_DAY));
	}

	@Transactional
	public ChronoSettings saveSettings(ChronoSettings s) {
		set(KEY_ENABLED, String.valueOf(s.enabled()));
		set(KEY_PRIZE, s.prize().trim());
		set(KEY_VALIDITY, String.valueOf(s.validityDays()));
		set(KEY_MAX_WINNERS, String.valueOf(s.maxWinnersPerDay()));
		set(KEY_ATTEMPTS, String.valueOf(s.attemptsPerDay()));
		return settings();
	}

	@Transactional(readOnly = true)
	public ChronoStats stats() {
		Instant today = startOfToday();
		Instant month = LocalDate.now(clock.withZone(zone)).minusDays(29).atStartOfDay(zone).toInstant();
		return new ChronoStats(attempts.countByStartedAtGreaterThanEqual(today), prizes.countByCreatedAtGreaterThanEqual(today),
				prizes.countByRedeemedAtGreaterThanEqual(today), attempts.countByStartedAtGreaterThanEqual(month),
				prizes.countByCreatedAtGreaterThanEqual(month), prizes.countByRedeemedAtGreaterThanEqual(month));
	}

	// --- Outils ---

	private PrizeView view(ChronoPrize p) {
		Instant now = Instant.now(clock);
		String status = p.getRedeemedAt() != null ? "REDEEMED" : p.getExpiresAt().isAfter(now) ? "PENDING" : "EXPIRED";
		return new PrizeView(p.getCode(), QR_PREFIX + p.getCode(), p.getLabel(), p.getElapsedMs(), p.getCreatedAt(), p.getExpiresAt(),
				status, p.getRedeemedAt(), p.getShop());
	}

	private ChronoPrize load(String raw) {
		String code = raw == null ? "" : raw.trim().toUpperCase();
		if (code.startsWith(QR_PREFIX)) {
			code = code.substring(QR_PREFIX.length());
		}
		code = code.replace("-", "").replace(" ", "");
		return prizes.findByCode(code).orElseThrow(() -> ApiException.notFound("Bon cadeau inconnu. Vérifiez les 7 caractères."));
	}

	private Instant startOfToday() {
		return LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
	}

	private String get(String key, String fallback) {
		return settings.findById(key).map(AppSetting::getValue).orElse(fallback);
	}

	private void set(String key, String value) {
		settings.findById(key).ifPresentOrElse(s -> s.setValue(value), () -> settings.save(new AppSetting(key, value)));
	}

	private static int intValue(String s, int fallback) {
		try {
			return s == null ? fallback : Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private String token() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 24; i++) {
			sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
		}
		return sb.toString();
	}

	private String newCode() {
		for (;;) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 7; i++) {
				sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
			}
			if (!prizes.existsByCode(sb.toString())) {
				return sb.toString();
			}
		}
	}
}
