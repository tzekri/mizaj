package ma.mizaj.otp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limite le nombre de demandes de code par adresse IP (chaque envoi WhatsApp est facturé par Twilio) :
 * au plus {@value #MAX_PER_HOUR} par heure. En mémoire : valable pour une instance.
 */
@Component
public class IpThrottle {

	static final int MAX_PER_HOUR = 15;
	private static final Duration WINDOW = Duration.ofHours(1);

	private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
	private final Clock clock;

	public IpThrottle(Clock clock) {
		this.clock = clock;
	}

	/** Enregistre une tentative et indique si elle dépasse la limite. */
	public boolean tryAcquire(String ip) {
		Deque<Instant> deque = hits.computeIfAbsent(ip, k -> new ArrayDeque<>());
		synchronized (deque) {
			prune(deque);
			if (deque.size() >= MAX_PER_HOUR) {
				return false;
			}
			deque.addLast(Instant.now(clock));
			return true;
		}
	}

	private void prune(Deque<Instant> deque) {
		Instant limit = Instant.now(clock).minus(WINDOW);
		while (!deque.isEmpty() && deque.peekFirst().isBefore(limit)) {
			deque.pollFirst();
		}
	}

	@Scheduled(fixedRate = 10 * 60 * 1000)
	void cleanup() {
		hits.entrySet().removeIf(e -> {
			synchronized (e.getValue()) {
				prune(e.getValue());
				return e.getValue().isEmpty();
			}
		});
	}
}
