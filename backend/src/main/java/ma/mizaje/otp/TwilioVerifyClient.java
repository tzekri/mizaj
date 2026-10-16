package ma.mizaje.otp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Twilio Verify : Twilio génère, envoie et vérifie le code (WhatsApp, SMS), avec anti-fraude intégré.
 * Appels HTTP directs (sans SDK). Doc : https://www.twilio.com/docs/verify/api
 * <p>
 * Le canal WhatsApp de Verify exige un expéditeur WhatsApp à vous, relié au service Verify
 * (« Bring Your Own WhatsApp Sender »). Tant qu'il ne l'est pas, Twilio refuse l'envoi WhatsApp :
 * on bascule alors automatiquement sur le canal suivant (SMS).
 */
public class TwilioVerifyClient {

	private static final Logger log = LoggerFactory.getLogger(TwilioVerifyClient.class);

	/**
	 * Erreurs Twilio qui signifient « ce canal ne peut pas servir » (pas « ce numéro est invalide ») :
	 * on essaie alors le canal suivant. 68008 / 63118 : WhatsApp non configuré dans Verify ;
	 * 60205 : SMS impossible vers ce numéro (fixe) ; 60223 : canal désactivé pour ce service.
	 */
	static final Set<Integer> CHANNEL_UNAVAILABLE = Set.of(68008, 63118, 60205, 60223);

	public enum CheckResult { APPROVED, WRONG_CODE, EXPIRED }

	private final RestClient http;
	private final String serviceSid;
	private final List<OtpChannel> channels;
	private final ObjectMapper json = new ObjectMapper();

	/** @param baseUrl https://verify.twilio.com/v2 (modifiable pour les tests) */
	public TwilioVerifyClient(String baseUrl, String accountSid, String authToken, String serviceSid, List<OtpChannel> channels) {
		this.serviceSid = serviceSid;
		this.channels = List.copyOf(channels);
		this.http = RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeaders(h -> h.setBasicAuth(accountSid, authToken))
				.build();
	}

	/** Canaux disponibles, dans l'ordre de préférence. */
	public List<OtpChannel> channels() {
		return channels;
	}

	/**
	 * Envoie un code. Si un canal est demandé, on commence par lui ; sinon par le canal préféré.
	 * Bascule sur les canaux suivants si Twilio indique que le canal n'est pas utilisable.
	 *
	 * @return le canal effectivement utilisé
	 */
	public OtpChannel start(String phone, OtpChannel requested) {
		List<OtpChannel> order = requested == null || !channels.contains(requested)
				? channels
				: Stream.concat(Stream.of(requested), channels.stream().filter(c -> c != requested)).toList();
		OtpDeliveryException last = null;
		for (OtpChannel channel : order) {
			try {
				post("/Services/{sid}/Verifications", form("To", phone, "Channel", channel.twilio, "Locale", "fr"));
				return channel;
			} catch (OtpDeliveryException e) {
				last = e;
				if (e.twilioCode() == null || !CHANNEL_UNAVAILABLE.contains(e.twilioCode())) {
					throw e;
				}
				log.warn("Twilio Verify : canal {} indisponible (erreur {}), essai du canal suivant.", channel, e.twilioCode());
			}
		}
		throw last;
	}

	public CheckResult check(String phone, String code) {
		try {
			JsonNode res = post("/Services/{sid}/VerificationCheck", form("To", phone, "Code", code));
			return "approved".equals(res.path("status").asText()) ? CheckResult.APPROVED : CheckResult.WRONG_CODE;
		} catch (OtpDeliveryException e) {
			// 404 / 20404 : aucune vérification en cours (expirée, déjà validée, ou trop d'essais).
			if (Integer.valueOf(20404).equals(e.twilioCode()) || Integer.valueOf(404).equals(e.httpStatus())) {
				return CheckResult.EXPIRED;
			}
			throw e;
		}
	}

	private JsonNode post(String uri, MultiValueMap<String, String> form) {
		try {
			String body = http.post()
					.uri(uri, serviceSid)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(String.class);
			return json.readTree(body == null ? "{}" : body);
		} catch (RestClientResponseException e) {
			Integer code = null;
			try {
				JsonNode err = json.readTree(e.getResponseBodyAsString());
				code = err.hasNonNull("code") ? err.get("code").asInt() : null;
			} catch (Exception ignored) {
				// corps non JSON
			}
			HttpStatusCode status = e.getStatusCode();
			throw new OtpDeliveryException("Twilio Verify : HTTP " + status.value() + (code == null ? "" : ", erreur " + code), e,
					code, status.value());
		} catch (RestClientException | java.io.IOException e) {
			throw new OtpDeliveryException("Twilio Verify injoignable", e, null, null);
		}
	}

	private static MultiValueMap<String, String> form(String... kv) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			form.add(kv[i], kv[i + 1]);
		}
		return form;
	}
}
