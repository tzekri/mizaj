package ma.mizaje.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import ma.mizaje.domain.ChronoAttempt;
import ma.mizaje.domain.ChronoPrize;
import ma.mizaje.repo.AppSettingRepository;
import ma.mizaje.repo.ChronoAttemptRepository;
import ma.mizaje.repo.ChronoPrizeRepository;
import ma.mizaje.repo.StaffUserRepository;
import ma.mizaje.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChronoServiceTest {

	private static final Instant START = Instant.parse("2026-10-27T10:00:00Z");
	private ChronoAttemptRepository attempts;
	private ChronoPrizeRepository prizes;

	@BeforeEach
	void setUp() {
		attempts = mock(ChronoAttemptRepository.class);
		prizes = mock(ChronoPrizeRepository.class);
		when(prizes.save(any(ChronoPrize.class))).thenAnswer(i -> i.getArgument(0));
		when(attempts.save(any(ChronoAttempt.class))).thenAnswer(i -> i.getArgument(0));
	}

	/** Service dont l'horloge marque START + serverMs, avec un essai démarré à START. */
	private ChronoService serviceAt(long serverMs) {
		Clock clock = Clock.fixed(START.plusMillis(serverMs), ZoneOffset.UTC);
		ChronoAttempt attempt = new ChronoAttempt("TOKEN", "device-123", "1.2.3.4", START);
		when(attempts.findByToken("TOKEN")).thenReturn(Optional.of(attempt));
		return new ChronoService(attempts, prizes, mock(AppSettingRepository.class), mock(StaffUserRepository.class), clock,
				ZoneId.of("Africa/Casablanca"));
	}

	@Test
	void onlyTenPointZeroZeroWins() {
		assertFalse(ChronoService.isWin(9_999));
		assertTrue(ChronoService.isWin(10_000));
		assertTrue(ChronoService.isWin(10_009));
		assertFalse(ChronoService.isWin(10_010));
	}

	@Test
	void winningStopGivesAPrize() {
		var result = serviceAt(10_120).stop("TOKEN", 10_004);
		assertTrue(result.win());
		assertEquals("Un café offert", result.prize().label());
		assertEquals("PENDING", result.prize().status());
		assertTrue(result.prize().qr().startsWith(ChronoService.QR_PREFIX));
	}

	@Test
	void losingStopGivesNothing() {
		var result = serviceAt(9_950).stop("TOKEN", 9_870);
		assertFalse(result.win());
		assertNull(result.prize());
	}

	@Test
	void announcingMoreTimeThanReallyElapsedIsRefused() {
		// Le téléphone annonce 10,00 s alors que seulement 4 s se sont écoulées côté serveur
		assertThrows(ApiException.class, () -> serviceAt(4_000).stop("TOKEN", 10_004));
	}

	@Test
	void threeAttemptsPerDayAndDevice() {
		ChronoService service = serviceAt(0);
		when(attempts.countByDeviceAndStartedAtGreaterThanEqual(eq("device-123"), any())).thenReturn(3L);
		assertThrows(ApiException.class, () -> service.start("device-123", "1.2.3.4"));
		when(attempts.countByDeviceAndStartedAtGreaterThanEqual(eq("device-456"), any())).thenReturn(1L);
		when(attempts.countByIpAndStartedAtGreaterThanEqual(anyString(), any())).thenReturn(0L);
		assertEquals(1, service.start("device-456", "1.2.3.4").remaining());
	}

	@Test
	void dailyWinnerCapStopsGivingPrizes() {
		ChronoService service = serviceAt(10_100);
		when(prizes.countByCreatedAtGreaterThanEqual(any())).thenReturn(20L);
		assertFalse(service.stop("TOKEN", 10_002).win());
	}
}
