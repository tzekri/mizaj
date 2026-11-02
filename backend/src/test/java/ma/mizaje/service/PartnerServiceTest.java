package ma.mizaje.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.Partner;
import ma.mizaje.domain.PartnerDisplay;
import ma.mizaje.repo.PartnerDisplayRepository;
import ma.mizaje.repo.PartnerPictureRepository;
import ma.mizaje.repo.PartnerRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.PartnerDtos.AdView;
import ma.mizaje.web.dto.PartnerDtos.PartnerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PartnerServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");
	private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

	private PartnerRepository partners;
	private PartnerDisplayRepository displays;
	private PartnerService service;

	@BeforeEach
	void setUp() {
		partners = mock(PartnerRepository.class);
		displays = mock(PartnerDisplayRepository.class);
		when(displays.save(any(PartnerDisplay.class))).thenAnswer(i -> i.getArgument(0));
		service = new PartnerService(partners, displays, mock(PartnerPictureRepository.class), Clock.fixed(NOW, ZONE), ZONE);
	}

	private static long nextId = 1;

	private static Partner partner(String name, String shops, LocalDate from, LocalDate to, boolean active) {
		Partner p = new Partner(0, NOW);
		ReflectionTestUtils.setField(p, "id", nextId++);
		p.update(name, "-10 %", null, shops, from, to, active, NOW);
		return p;
	}

	@Test
	void tvShowsOnlyLiveAdsForItsShop() {
		when(partners.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
				partner("Partout", null, null, null, true),
				partner("Lissasfa seulement", "Lissasfa", null, null, true),
				partner("Farah seulement", "Farah Salam", null, null, true),
				partner("En pause", null, null, null, false),
				partner("Terminé", null, TODAY.minusDays(10), TODAY.minusDays(1), true),
				partner("Pas commencé", null, TODAY.plusDays(1), null, true),
				partner("Dernier jour", null, TODAY.minusDays(3), TODAY, true)));
		assertEquals(List.of("Partout", "Lissasfa seulement", "Dernier jour"), names(service.ads("Lissasfa")));
		assertEquals(List.of("Partout", "Lissasfa seulement", "Farah seulement", "Dernier jour"), names(service.ads(null)));
	}

	@Test
	void sameTvDoesNotCountTheSameAdTwiceWithinFiveSeconds() {
		Partner p = partner("Sushi", null, null, null, true);
		when(partners.findById(1L)).thenReturn(Optional.of(p));
		PartnerDisplay row = new PartnerDisplay(p, TODAY, "Lissasfa");
		when(displays.findByPartnerAndDayAndShop(p, TODAY, "Lissasfa")).thenReturn(Optional.of(row));
		service.shown(1, "Lissasfa", "TV1");
		service.shown(1, "Lissasfa", "TV1");
		service.shown(1, "Lissasfa", "TV2");
		assertEquals(2, row.getShown());
	}

	@Test
	void unknownShopNamesAreCountedWithoutShop() {
		Partner p = partner("Sushi", null, null, null, true);
		when(partners.findById(1L)).thenReturn(Optional.of(p));
		when(displays.findByPartnerAndDayAndShop(any(), any(), any())).thenReturn(Optional.empty());
		service.shown(1, "<script>", "TV1");
		verify(displays, times(1)).findByPartnerAndDayAndShop(p, TODAY, PartnerDisplay.NO_SHOP);
		verify(displays, never()).findByPartnerAndDayAndShop(p, TODAY, "<script>");
	}

	@Test
	void imageMustBeAPicture() {
		when(partners.save(any(Partner.class))).thenAnswer(i -> i.getArgument(0));
		PartnerRequest bad = new PartnerRequest("Sushi", "-10 %", null, List.of(), null, null, true, "data:text/html;base64,PGgxPg==");
		assertThrows(ApiException.class, () -> service.create(bad));
	}

	@Test
	void endBeforeStartIsRefused() {
		when(partners.save(any(Partner.class))).thenAnswer(i -> i.getArgument(0));
		PartnerRequest req = new PartnerRequest("Sushi", "-10 %", null, List.of(), TODAY, TODAY.minusDays(1), true, null);
		assertThrows(ApiException.class, () -> service.create(req));
	}

	@Test
	void liveMeansActiveAndWithinDates() {
		assertTrue(PartnerService.live(partner("a", null, TODAY, TODAY, true), TODAY));
		assertFalse(PartnerService.live(partner("a", null, null, null, false), TODAY));
	}

	private static List<String> names(List<AdView> ads) {
		return ads.stream().map(AdView::name).toList();
	}
}
