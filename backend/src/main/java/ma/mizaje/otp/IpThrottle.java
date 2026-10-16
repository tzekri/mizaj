package ma.mizaje.otp;

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
 * Limite le nombre de demandes (codes, inscriptions) par adresse IP — chaque envoi par Twilio est facturé :
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
		return tryAcquire(ip, MAX_PER_HOUR);
	}

	/**
	 * Variante avec sa propre limite horaire, comptée à part (clé préfixée par l'appelant).
	 * Ex. inscriptions : plus permissif, car tous les clients d'un coffee shop partagent l'IP du Wi-Fi.
	 */
	public boolean tryAcquire(String key, int maxPerHour) {
		Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (deque) {
			prune(deque);
			if (deque.size() >= maxPerHour) {
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
