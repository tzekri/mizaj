package ma.mizaje.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.WeeklyAd;
import ma.mizaje.domain.WeeklyAdPicture;
import ma.mizaje.repo.WeeklyAdPictureRepository;
import ma.mizaje.repo.WeeklyAdRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdImage;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdPublic;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdRequest;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pub de la semaine : celle du jour pour la page /promo, et la programmation des semaines par l'administrateur. */
@Service
public class WeeklyAdService {

	private final WeeklyAdRepository ads;
	private final WeeklyAdPictureRepository pictures;
	private final Clock clock;
	private final ZoneId zone;

	public WeeklyAdService(WeeklyAdRepository ads, WeeklyAdPictureRepository pictures, Clock clock, ZoneId appZone) {
		this.ads = ads;
		this.pictures = pictures;
		this.clock = clock;
		this.zone = appZone;
	}

	/** Pub affichée aujourd'hui : active, avec une affiche, dans sa période ; la plus récente si plusieurs se chevauchent. */
	@Transactional(readOnly = true)
	public Optional<WeeklyAdPublic> current() {
		LocalDate today = today();
		return ads.findAllByOrderByStartsOnDescIdDesc().stream()
				.filter(a -> "CURRENT".equals(status(a, today)))
				.findFirst()
				.map(a -> new WeeklyAdPublic(a.getId(), a.getTitle(), a.getDescription(), imageUrl(a), a.getEndsOn()));
	}

	@Transactional(readOnly = true)
	public WeeklyAdImage image(long id) {
		WeeklyAd ad = ad(id);
		WeeklyAdPicture picture = pictures.findById(id).orElseThrow(() -> ApiException.notFound("Pas d'affiche."));
		return new WeeklyAdImage(picture.getData(), ad.getImageType());
	}

	// --- Administration ---

	@Transactional(readOnly = true)
	public List<WeeklyAdView> list() {
		LocalDate today = today();
		// Une seule pub « affichée » : la première en cours dans l'ordre (la plus récente)
		boolean[] shown = {false};
		return ads.findAllByOrderByStartsOnDescIdDesc().stream().map(a -> {
			String status = status(a, today);
			if ("CURRENT".equals(status)) {
				status = shown[0] ? "OVERLAP" : "CURRENT";
				shown[0] = true;
			}
			return new WeeklyAdView(a.getId(), a.getTitle(), a.getDescription(), a.getStartsOn(), a.getEndsOn(), a.isActive(), status,
					imageUrl(a));
		}).toList();
	}

	@Transactional
	public WeeklyAdView create(WeeklyAdRequest req) {
		if (req.image() == null || req.image().isBlank()) {
			throw ApiException.badRequest("Ajoutez l'image de l'affiche.");
		}
		return save(new WeeklyAd(Instant.now(clock)), req);
	}

	@Transactional
	public WeeklyAdView update(long id, WeeklyAdRequest req) {
		return save(ad(id), req);
	}

	@Transactional
	public void delete(long id) {
		WeeklyAd ad = ad(id);
		pictures.findById(id).ifPresent(pictures::delete);
		ads.delete(ad);
	}

	// --- Outils ---

	private WeeklyAdView save(WeeklyAd ad, WeeklyAdRequest req) {
		if (req.endsOn().isBefore(req.startsOn())) {
			throw ApiException.badRequest("La date de fin est avant la date de début.");
		}
		Instant now = Instant.now(clock);
		String description = req.description() == null || req.description().isBlank() ? null : req.description().trim();
		ad.update(req.title().trim(), description, req.startsOn(), req.endsOn(), req.active(), now);
		WeeklyAd saved = ads.save(ad); // nouvelle pub : l'affiche a besoin de son identifiant
		if (req.image() != null && !req.image().isBlank()) {
			DataImages.Decoded image = DataImages.decode(req.image().trim());
			pictures.findById(saved.getId()).ifPresentOrElse(p -> p.setData(image.data()),
					() -> pictures.save(new WeeklyAdPicture(saved.getId(), image.data())));
			saved.setImageType(image.type(), now);
		}
		return list().stream().filter(v -> v.id() == saved.getId()).findFirst().orElseThrow();
	}

	static String status(WeeklyAd a, LocalDate today) {
		if (!a.hasImage()) {
			return "NO_IMAGE";
		}
		if (!a.isActive()) {
			return "PAUSED";
		}
		if (today.isBefore(a.getStartsOn())) {
			return "SCHEDULED";
		}
		return today.isAfter(a.getEndsOn()) ? "ENDED" : "CURRENT";
	}

	private static String imageUrl(WeeklyAd a) {
		return a.hasImage() ? "/api/public/weekly-ad/" + a.getId() + "/image?v=" + a.getUpdatedAt().toEpochMilli() : null;
	}

	private WeeklyAd ad(long id) {
		return ads.findById(id).orElseThrow(() -> ApiException.notFound("Pub introuvable."));
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(zone));
	}
}
