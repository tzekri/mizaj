package ma.mizaj.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

	private static final Instant START = Instant.parse("2026-10-01T09:00:00Z");
	private static final StaffUser BARISTA = new StaffUser("B", "b@mizaj.ma", "x", StaffRole.BARISTA, "Maârif");

	private Subscription newSubscription(int drinks) {
		Customer customer = new Customer("+212612345678", "T", "token", START);
		return new Subscription(customer, START, START.plus(Duration.ofDays(30)), drinks, 120, BARISTA);
	}

	@Test
	void activeUntilThirtyDaysThenExpired() {
		Subscription s = newSubscription(30);
		assertEquals(SubscriptionStatus.ACTIVE, s.statusAt(START.plus(Duration.ofDays(29))));
		assertEquals(SubscriptionStatus.EXPIRED, s.statusAt(START.plus(Duration.ofDays(30))));
	}

	@Test
	void emptyWhenAllDrinksConsumed() {
		Subscription s = newSubscription(2);
		s.consumeOne();
		assertEquals(SubscriptionStatus.ACTIVE, s.statusAt(START));
		s.consumeOne();
		assertEquals(SubscriptionStatus.EMPTY, s.statusAt(START));
	}

	@Test
	void giveBackNeverExceedsTotal() {
		Subscription s = newSubscription(30);
		s.giveBackOne();
		assertEquals(30, s.getDrinksLeft());
	}

	@Test
	void closingEarlyExpiresTheRemainingDrinks() {
		Subscription s = newSubscription(30);
		Instant renewal = START.plus(Duration.ofDays(10));
		s.closeAt(renewal);
		assertEquals(SubscriptionStatus.EXPIRED, s.statusAt(renewal));
	}
}
