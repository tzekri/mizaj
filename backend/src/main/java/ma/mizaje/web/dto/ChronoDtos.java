package ma.mizaje.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Défi « 10 secondes » : requêtes et réponses JSON. */
public final class ChronoDtos {

	private ChronoDtos() {
	}

	/** Règles affichées au joueur. remaining : essais restants aujourd'hui sur cet appareil. */
	public record ChronoInfo(boolean enabled, String prize, int attemptsPerDay, int remaining, int validityDays) {
	}

	/**
	 * device : identifiant aléatoire de l'appareil, gardé dans le navigateur.
	 * screen : écran géant sur lequel jouer (téléphone manette), ou absent.
	 */
	public record StartRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9-]{8,64}") String device,
			@Pattern(regexp = "[A-Za-z0-9]{6,16}") String screen) {
	}

	public record StartView(String token, int remaining) {
	}

	/**
	 * elapsedMs : temps mesuré par le téléphone entre « Démarrer » et « Arrêter ».
	 * latencyMs : sur écran géant, moitié de l'aller-retour réseau mesuré par le téléphone.
	 */
	public record StopRequest(@NotBlank @Size(max = 32) String token, @Min(0) @Max(600_000) int elapsedMs,
			@Min(0) @Max(5_000) Integer latencyMs) {
	}

	/** status : PENDING (à remettre), REDEEMED (déjà remis), EXPIRED. qr : contenu du QR code. */
	public record PrizeView(String code, String qr, String label, int elapsedMs, Instant createdAt, Instant expiresAt, String status,
			Instant redeemedAt, String shop) {
	}

	/** prize : bon gagné si win, sinon null. */
	public record StopView(int elapsedMs, boolean win, PrizeView prize, int remaining) {
	}

	public record PrizeScanRequest(@NotBlank @Size(max = 40) String code) {
	}

	/** Réglages de l'administrateur. maxWinnersPerDay : nombre de bons au maximum par jour (tous appareils confondus). */
	public record ChronoSettings(
			boolean enabled,
			@NotBlank @Size(max = 80) String prize,
			@Min(1) @Max(90) int validityDays,
			@Min(1) @Max(1000) int maxWinnersPerDay) {
	}

	public record ChronoStats(long attemptsToday, long winnersToday, long redeemedToday, long attempts30, long winners30, long redeemed30) {
	}
}
