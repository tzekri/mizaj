package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Essai au défi « 10 secondes » : départ côté serveur, temps mesuré par le téléphone à l'arrêt. */
@Entity
@Table(name = "chrono_attempts")
public class ChronoAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Jeton à usage unique remis au départ, renvoyé à l'arrêt. */
	@Column(nullable = false, unique = true, length = 32)
	private String token;

	/** Identifiant aléatoire de l'appareil (stocké dans le navigateur) : 3 essais par jour et par appareil. */
	@Column(nullable = false, length = 64)
	private String device;

	@Column(nullable = false, length = 64)
	private String ip;

	@Column(nullable = false)
	private Instant startedAt;

	private Instant stoppedAt;

	/** Temps affiché au joueur, en millisecondes. */
	private Integer elapsedMs;

	@Column(nullable = false)
	private boolean win;

	/** Écran géant sur lequel la partie est affichée (téléphone manette) ; null : partie sur le téléphone seul. */
	@Column(length = 16)
	private String screen;

	protected ChronoAttempt() {
	}

	public ChronoAttempt(String token, String device, String ip, Instant startedAt, String screen) {
		this(token, device, ip, startedAt);
		this.screen = screen;
	}

	public ChronoAttempt(String token, String device, String ip, Instant startedAt) {
		this.token = token;
		this.device = device;
		this.ip = ip;
		this.startedAt = startedAt;
	}

	public void stop(Instant at, Integer elapsedMs, boolean win) {
		this.stoppedAt = at;
		this.elapsedMs = elapsedMs;
		this.win = win;
	}

	public Long getId() { return id; }
	public String getToken() { return token; }
	public String getDevice() { return device; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getStoppedAt() { return stoppedAt; }
	public Integer getElapsedMs() { return elapsedMs; }
	public boolean isWin() { return win; }
	public String getScreen() { return screen; }
}
