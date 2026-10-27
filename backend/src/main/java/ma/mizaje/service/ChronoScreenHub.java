package ma.mizaje.service;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Écrans géants du défi « 10 secondes » : état de la partie en cours sur chaque écran, diffusé en direct (SSE)
 * à la TV et aux téléphones manettes. En mémoire : un écran se recrée de lui-même après un redémarrage.
 */
@Component
public class ChronoScreenHub {

	/** Une partie sans « Arrêter » depuis plus longtemps libère l'écran. */
	static final long RUN_TIMEOUT_MS = 30_000;
	/** Durée d'une connexion SSE ; le navigateur se reconnecte tout seul ensuite. */
	static final long EMITTER_TIMEOUT_MS = 30 * 60_000L;

	/**
	 * État d'un écran. status : IDLE (libre), RUNNING (partie en cours), RESULT (dernier résultat affiché).
	 * startedAt : heure serveur du départ (ms) ; changedAt : heure serveur du dernier changement (un vieux résultat n'est pas
	 * réaffiché) ; serverNow : heure serveur à l'envoi, pour caler l'horloge de la TV.
	 */
	public record ScreenState(String status, long startedAt, Integer elapsedMs, boolean win, String prize, long changedAt,
			long serverNow) {
	}

	private record Game(String status, String token, long startedAt, Integer elapsedMs, boolean win, String prize, long changedAt) {
	}

	private static final Game IDLE = new Game("IDLE", null, 0, null, false, null, 0);

	private final Map<String, Game> games = new ConcurrentHashMap<>();
	private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
	private final Clock clock;

	public ChronoScreenHub(Clock clock) {
		this.clock = clock;
	}

	/** Connexion d'un écran ou d'une manette : reçoit l'état actuel tout de suite, puis chaque changement. */
	public SseEmitter subscribe(String screen) {
		SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
		List<SseEmitter> list = emitters.computeIfAbsent(screen, k -> new CopyOnWriteArrayList<>());
		list.add(emitter);
		Runnable remove = () -> list.remove(emitter);
		emitter.onCompletion(remove);
		emitter.onTimeout(remove);
		emitter.onError(e -> remove.run());
		send(screen, emitter, SseEmitter.event().name("state").data(state(screen)));
		return emitter;
	}

	/** Une partie est-elle en cours sur cet écran ? */
	public boolean busy(String screen) {
		Game g = games.getOrDefault(screen, IDLE);
		return "RUNNING".equals(g.status()) && clock.millis() - g.startedAt() < RUN_TIMEOUT_MS;
	}

	public void running(String screen, String token, long startedAt) {
		games.put(screen, new Game("RUNNING", token, startedAt, null, false, null, clock.millis()));
		broadcast(screen);
	}

	/** Résultat de la partie (ignoré si une autre partie a déjà pris l'écran). */
	public void result(String screen, String token, int elapsedMs, boolean win, String prize) {
		Game g = games.get(screen);
		if (g != null && token.equals(g.token())) {
			games.put(screen, new Game("RESULT", token, g.startedAt(), elapsedMs, win, prize, clock.millis()));
			broadcast(screen);
		}
	}

	public ScreenState state(String screen) {
		Game g = games.getOrDefault(screen, IDLE);
		if ("RUNNING".equals(g.status()) && !busy(screen)) {
			g = IDLE; // partie abandonnée
		}
		return new ScreenState(g.status(), g.startedAt(), g.elapsedMs(), g.win(), g.prize(), g.changedAt(), clock.millis());
	}

	private void broadcast(String screen) {
		ScreenState s = state(screen);
		emitters.getOrDefault(screen, List.of()).forEach(e -> send(screen, e, SseEmitter.event().name("state").data(s)));
	}

	/**
	 * Envoi à une TV ou une manette. Une connexion coupée (TV éteinte, page rechargée) est retirée sans bruit :
	 * elle ne doit jamais faire échouer la partie des autres joueurs.
	 */
	private void send(String screen, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
		} catch (IOException | RuntimeException e) {
			drop(screen, emitter);
		}
	}

	private void drop(String screen, SseEmitter emitter) {
		emitters.getOrDefault(screen, List.of()).remove(emitter);
		try {
			emitter.complete();
		} catch (RuntimeException ignored) {
			// connexion déjà fermée côté serveur d'application
		}
	}

	/** Signe de vie régulier : garde les connexions ouvertes derrière les proxys et libère les parties abandonnées. */
	@Scheduled(fixedRate = 20_000)
	public void heartbeat() {
		emitters.forEach((screen, list) -> {
			Game g = games.get(screen);
			if (g != null && "RUNNING".equals(g.status()) && !busy(screen)) {
				games.put(screen, IDLE);
				broadcast(screen);
			}
			for (SseEmitter e : list) {
				send(screen, e, SseEmitter.event().comment("ping"));
			}
		});
	}
}
