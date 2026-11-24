package ma.mizaje.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import ma.mizaje.domain.WeeklyAd;
import ma.mizaje.repo.WeeklyAdPictureRepository;
import ma.mizaje.repo.WeeklyAdRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.WeeklyAdDtos.WeeklyAdRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WeeklyAdServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");
	private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z"); // mercredi
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
	private static long nextId = 1;

	private static WeeklyAd ad(String title, LocalDate from, LocalDate to, boolean active, boolean image) {
		WeeklyAd a = new WeeklyAd(NOW);
		ReflectionTestUtils.setField(a, "id", nextId++);
		a.update(title, null, from, to, active, NOW);
		if (image) {
			a.setImageType("image/jpeg", NOW);
		}
		return a;
	}

	private static WeeklyAdService service(List<WeeklyAd> all) {
		WeeklyAdRepository ads = mock(WeeklyAdRepository.class);
		when(ads.findAllByOrderByStartsOnDescIdDesc()).thenReturn(all);
		return new WeeklyAdService(ads, mock(WeeklyAdPictureRepository.class), Clock.fixed(NOW, ZONE), ZONE);
	}

	@Test
	void pageShowsThisWeeksAdAmongPastAndScheduledOnes() {
		LocalDate monday = TODAY.minusDays(2);
		WeeklyAdService s = service(List.of(
				ad("Semaine prochaine", monday.plusDays(7), monday.plusDays(13), true, true),
				ad("Affogato", monday, monday.plusDays(6), true, true),
				ad("Semaine dernière", monday.minusDays(7), monday.minusDays(1), true, true)));
		assertEquals("Affogato", s.current().orElseThrow().title());
		assertEquals(List.of("SCHEDULED", "CURRENT", "ENDED"), s.list().stream().map(v -> v.status()).toList());
	}

	@Test
	void pausedOrImagelessAdsAreNotShown() {
		WeeklyAdService s = service(List.of(
				ad("En pause", TODAY, TODAY, false, true),
				ad("Sans affiche", TODAY, TODAY, true, false)));
		assertTrue(s.current().isEmpty());
	}

	@Test
	void anAdNeedsItsPicture() {
		WeeklyAdService s = service(List.of());
		assertThrows(ApiException.class, () -> s.create(new WeeklyAdRequest("Affogato", null, TODAY, TODAY.plusDays(6), true, null)));
	}
}
