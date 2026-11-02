package ma.mizaje.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Partenaires et leurs pubs sur l'écran géant : requêtes et réponses JSON. */
public final class PartnerDtos {

	private PartnerDtos() {
	}

	/** Pub telle que la diffuse la TV. image : adresse de l'image, ou null. */
	public record AdView(long id, String name, String offer, String details, String image) {
	}

	/**
	 * Partenaire vu par l'administrateur. shops : coffee shops de diffusion (vide : tous) ; live : diffusé aujourd'hui
	 * (actif et dans sa période) ; shown* : nombre d'affichages sur les TV.
	 */
	public record PartnerView(long id, String name, String offer, String details, List<String> shops, LocalDate startsOn,
			LocalDate endsOn, boolean active, boolean live, String image, long shownToday, long shown30, long shownTotal) {
	}

	/**
	 * image : null garde l'image actuelle, "" la retire, sinon image en « data URL » (JPEG, PNG ou WebP).
	 */
	public record PartnerRequest(
			@NotBlank @Size(max = 60) String name,
			@NotBlank @Size(max = 120) String offer,
			@Size(max = 200) String details,
			@Size(max = 10) List<@Size(max = 60) String> shops,
			LocalDate startsOn,
			LocalDate endsOn,
			boolean active,
			@Size(max = 4_000_000) String image) {
	}

	/** Image d'un partenaire, à servir telle quelle. */
	public record PartnerImage(byte[] data, String type) {
	}
}
