package ma.mizaje.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ma.mizaje.domain.Partner;
import ma.mizaje.domain.PartnerDisplay;
import ma.mizaje.domain.PartnerPicture;
import ma.mizaje.repo.PartnerDisplayRepository;
import ma.mizaje.repo.PartnerPictureRepository;
import ma.mizaje.repo.PartnerRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.PartnerDtos.AdView;
import ma.mizaje.web.dto.PartnerDtos.PartnerImage;
import ma.mizaje.web.dto.PartnerDtos.PartnerRequest;
import ma.mizaje.web.dto.PartnerDtos.PartnerView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pubs des partenaires sur l'écran géant du défi : diffusion (TV), comptage des affichages et gestion par l'administrateur.
 */
@Service
public class PartnerService {

	/** Une même TV ne compte pas deux fois la même pub à moins de 5 secondes d'écart (page rechargée, double envoi). */
	static final long MIN_GAP_MS = 5_000;
	static final int MAX_IMAGE_BYTES = 2_500_000;
	private static final Pattern DATA_URL = Pattern.compile("^data:(image/(?:jpeg|png|webp));base64,([A-Za-z0-9+/=\\s]+)$");
	private static final Pattern SHOP_NAME = Pattern.compile("[\\p{L}0-9 '’-]{1,60}");

	private final PartnerRepository partners;
	private final PartnerDisplayRepository displays;
	private final PartnerPictureRepository pictures;
	private final Clock clock;
	private final ZoneId zone;
	private final Map<String, Long> lastShown = new ConcurrentHashMap<>();

	public PartnerService(PartnerRepository partners, PartnerDisplayRepository displays, PartnerPictureRepository pictures, Clock clock,
			ZoneId appZone) {
		this.partners = partners;
		this.displays = displays;
		this.pictures = pictures;
		this.clock = clock;
		this.zone = appZone;
	}

	// --- Écran géant ---

	/** Pubs à diffuser aujourd'hui sur la TV d'un coffee shop (shop null : écran sans boutique, toutes les pubs). */
	@Transactional(readOnly = true)
	public List<AdView> ads(String shop) {
		LocalDate today = today();
		return partners.findAllByOrderBySortOrderAscIdAsc().stream()
				.filter(p -> live(p, today))
				.filter(p -> shop == null || p.getShops() == null || shops(p).contains(shop))
				.map(p -> new AdView(p.getId(), p.getName(), p.getOffer(), p.getDetails(), imageUrl(p)))
				.toList();
	}

	/** La TV signale qu'elle vient d'afficher une pub. */
	@Transactional
	public void shown(long id, String shop, String screen) {
		long now = clock.millis();
		Long before = lastShown.put(screen + ":" + id, now);
		if (before != null && now - before < MIN_GAP_MS) {
			return;
		}
		Partner partner = partners.findById(id).orElse(null);
		if (partner == null) {
			return;
		}
		String where = shop != null && SHOP_NAME.matcher(shop).matches() ? shop : PartnerDisplay.NO_SHOP;
		LocalDate today = today();
		displays.findByPartnerAndDayAndShop(partner, today, where)
				.orElseGet(() -> displays.save(new PartnerDisplay(partner, today, where)))
				.increment();
	}

	@Transactional(readOnly = true)
	public PartnerImage image(long id) {
		Partner p = partner(id);
		PartnerPicture picture = pictures.findById(id).orElseThrow(() -> ApiException.notFound("Pas d'image."));
		return new PartnerImage(picture.getData(), p.getImageType());
	}

	// --- Administration ---

	@Transactional(readOnly = true)
	public List<PartnerView> list() {
		return partners.findAllByOrderBySortOrderAscIdAsc().stream().map(this::view).toList();
	}

	@Transactional
	public PartnerView create(PartnerRequest req) {
		return save(new Partner((int) partners.count(), Instant.now(clock)), req);
	}

	@Transactional
	public PartnerView update(long id, PartnerRequest req) {
		return save(partner(id), req);
	}

	@Transactional
	public void delete(long id) {
		Partner p = partner(id);
		displays.deleteByPartner(p);
		pictures.findById(id).ifPresent(pictures::delete);
		partners.delete(p);
	}

	/** Ordre de passage sur la TV : échange le partenaire avec son voisin, puis renumérote (0, 1, 2…). */
	@Transactional
	public void move(long id, boolean up) {
		List<Partner> list = new ArrayList<>(partners.findAllByOrderBySortOrderAscIdAsc());
		Partner p = partner(id);
		int i = list.indexOf(p);
		int j = up ? i - 1 : i + 1;
		if (i >= 0 && j >= 0 && j < list.size()) {
			list.set(i, list.get(j));
			list.set(j, p);
		}
		for (int k = 0; k < list.size(); k++) {
			list.get(k).setSortOrder(k);
		}
	}

	// --- Outils ---

	private PartnerView save(Partner p, PartnerRequest req) {
		if (req.startsOn() != null && req.endsOn() != null && req.endsOn().isBefore(req.startsOn())) {
			throw ApiException.badRequest("La date de fin est avant la date de début.");
		}
		List<String> shops = req.shops() == null ? List.of()
				: req.shops().stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
		if (shops.stream().anyMatch(s -> !SHOP_NAME.matcher(s).matches())) {
			throw ApiException.badRequest("Nom de coffee shop invalide.");
		}
		Instant now = Instant.now(clock);
		p.update(req.name().trim(), req.offer().trim(), blankToNull(req.details()), shops.isEmpty() ? null : String.join(",", shops),
				req.startsOn(), req.endsOn(), req.active(), now);
		p = partners.save(p); // nouveau partenaire : l'image a besoin de son identifiant
		if (req.image() != null) {
			setImage(p, req.image().trim(), now);
		}
		return view(p);
	}

	private void setImage(Partner p, String dataUrl, Instant now) {
		if (dataUrl.isEmpty()) {
			pictures.findById(p.getId()).ifPresent(pictures::delete);
			p.setImageType(null, now);
			return;
		}
		Matcher m = DATA_URL.matcher(dataUrl);
		if (!m.matches()) {
			throw ApiException.badRequest("Image non reconnue : utilisez une photo JPEG, PNG ou WebP.");
		}
		byte[] data;
		try {
			data = Base64.getMimeDecoder().decode(m.group(2));
		} catch (IllegalArgumentException e) {
			throw ApiException.badRequest("Image illisible.");
		}
		if (data.length > MAX_IMAGE_BYTES) {
			throw ApiException.badRequest("Image trop lourde (2,5 Mo au maximum).");
		}
		pictures.findById(p.getId()).ifPresentOrElse(pic -> pic.setData(data), () -> pictures.save(new PartnerPicture(p.getId(), data)));
		p.setImageType(m.group(1), now);
	}

	private PartnerView view(Partner p) {
		LocalDate today = today();
		return new PartnerView(p.getId(), p.getName(), p.getOffer(), p.getDetails(), shops(p), p.getStartsOn(), p.getEndsOn(),
				p.isActive(), live(p, today), imageUrl(p), displays.countShownSince(p, today),
				displays.countShownSince(p, today.minusDays(29)), displays.countShownSince(p, LocalDate.of(2000, 1, 1)));
	}

	/** Actif et dans sa période de diffusion. */
	static boolean live(Partner p, LocalDate today) {
		return p.isActive() && (p.getStartsOn() == null || !today.isBefore(p.getStartsOn()))
				&& (p.getEndsOn() == null || !today.isAfter(p.getEndsOn()));
	}

	private static List<String> shops(Partner p) {
		return p.getShops() == null ? List.of() : Arrays.asList(p.getShops().split(","));
	}

	/** Adresse de l'image, versionnée : une TV recharge l'image seulement quand elle a changé. */
	private static String imageUrl(Partner p) {
		return p.hasImage() ? "/api/public/partners/" + p.getId() + "/image?v=" + p.getUpdatedAt().toEpochMilli() : null;
	}

	private Partner partner(long id) {
		return partners.findById(id).orElseThrow(() -> ApiException.notFound("Partenaire introuvable."));
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(zone));
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
