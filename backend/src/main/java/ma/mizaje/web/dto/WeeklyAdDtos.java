package ma.mizaje.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Pub de la semaine : requêtes et réponses JSON. */
public final class WeeklyAdDtos {

	private WeeklyAdDtos() {
	}

	/** Pub affichée sur la page /promo. image : adresse de l'affiche. */
	public record WeeklyAdPublic(long id, String title, String description, String image, LocalDate endsOn) {
	}

	/** Pub vue par l'administrateur ; status : CURRENT (affichée), SCHEDULED (à venir), ENDED, PAUSED, NO_IMAGE. */
	public record WeeklyAdView(long id, String title, String description, LocalDate startsOn, LocalDate endsOn, boolean active,
			String status, String image) {
	}

	/** image : null garde l'affiche actuelle, sinon affiche en « data URL » (JPEG, PNG ou WebP). */
	public record WeeklyAdRequest(
			@NotBlank @Size(max = 60) String title,
			@Size(max = 160) String description,
			@NotNull LocalDate startsOn,
			@NotNull LocalDate endsOn,
			boolean active,
			@Size(max = 4_000_000) String image) {
	}

	/** Affiche à servir telle quelle. */
	public record WeeklyAdImage(byte[] data, String type) {
	}
}
