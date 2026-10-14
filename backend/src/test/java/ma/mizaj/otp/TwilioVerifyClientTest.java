package ma.mizaj.otp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Client Twilio Verify contre un faux serveur HTTP (aucun appel réel à Twilio). */
class TwilioVerifyClientTest {

	private HttpServer server;
	private final List<String> requests = new ArrayList<>();

	/** Réponse simulée : statut HTTP + corps JSON, selon le corps (formulaire) de la requête. */
	private record Reply(int status, String body) {
	}

	private TwilioVerifyClient client(Function<String, Reply> handler) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			String form = URLDecoder.decode(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), StandardCharsets.UTF_8);
			requests.add(exchange.getRequestURI().getPath() + "?" + form);
			Reply reply = handler.apply(form);
			byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(reply.status(), bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		return new TwilioVerifyClient("http://127.0.0.1:" + server.getAddress().getPort(), "AC1", "tok", "VA1",
				List.of(OtpChannel.WHATSAPP, OtpChannel.SMS));
	}

	@AfterEach
	void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void sendsOnWhatsappWhenAvailable() throws IOException {
		TwilioVerifyClient c = client(f -> new Reply(201, "{\"status\":\"pending\"}"));
		assertEquals(OtpChannel.WHATSAPP, c.start("+212612345678", null));
		assertEquals(1, requests.size());
		assertTrue(requests.get(0).startsWith("/Services/VA1/Verifications?"));
		assertTrue(requests.get(0).contains("Channel=whatsapp") && requests.get(0).contains("To=+212612345678"));
	}

	@Test
	void fallsBackToSmsWhenWhatsappIsNotConfigured() throws IOException {
		TwilioVerifyClient c = client(f -> f.contains("Channel=whatsapp")
				? new Reply(400, "{\"code\":68008,\"message\":\"Verify WhatsApp channel not configured\"}")
				: new Reply(201, "{\"status\":\"pending\"}"));
		assertEquals(OtpChannel.SMS, c.start("+212612345678", null));
		assertEquals(2, requests.size());
	}

	@Test
	void customerCanAskForSmsFirst() throws IOException {
		TwilioVerifyClient c = client(f -> new Reply(201, "{\"status\":\"pending\"}"));
		assertEquals(OtpChannel.SMS, c.start("+212612345678", OtpChannel.SMS));
		assertTrue(requests.get(0).contains("Channel=sms"));
	}

	@Test
	void invalidNumberIsNotRetriedOnAnotherChannel() throws IOException {
		TwilioVerifyClient c = client(f -> new Reply(400, "{\"code\":60200,\"message\":\"Invalid parameter\"}"));
		OtpDeliveryException e = assertThrows(OtpDeliveryException.class, () -> c.start("+212612345678", null));
		assertEquals(60200, e.twilioCode());
		assertEquals(1, requests.size());
	}

	@Test
	void checkMapsTwilioStatuses() throws IOException {
		TwilioVerifyClient c = client(f -> f.contains("Code=123456") ? new Reply(200, "{\"status\":\"approved\"}")
				: f.contains("Code=000000") ? new Reply(404, "{\"code\":20404}")
				: new Reply(200, "{\"status\":\"pending\"}"));
		assertEquals(TwilioVerifyClient.CheckResult.APPROVED, c.check("+212612345678", "123456"));
		assertEquals(TwilioVerifyClient.CheckResult.WRONG_CODE, c.check("+212612345678", "999999"));
		assertEquals(TwilioVerifyClient.CheckResult.EXPIRED, c.check("+212612345678", "000000"));
		assertTrue(requests.get(0).startsWith("/Services/VA1/VerificationCheck?"));
	}
}
