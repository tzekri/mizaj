package ma.mizaj.otp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Envoi du code par WhatsApp via l'API Messages de Twilio (appel HTTP direct, sans SDK).
 * <p>
 * En production, WhatsApp impose un modèle de message approuvé pour écrire en premier à un client :
 * renseigner {@code TWILIO_CONTENT_SID} (modèle « authentication » avec la variable {{1}} = code).
 * Sans modèle, un texte libre est envoyé : accepté uniquement par la sandbox Twilio.
 */
public class TwilioWhatsAppSender implements OtpSender {

	private final RestClient http;
	private final String accountSid;
	private final String from;
	private final String contentSid;
	private final ObjectMapper json = new ObjectMapper();

	public TwilioWhatsAppSender(String accountSid, String authToken, String from, String contentSid) {
		this.accountSid = accountSid;
		this.from = from.startsWith("whatsapp:") ? from : "whatsapp:" + from;
		this.contentSid = contentSid;
		this.http = RestClient.builder()
				.baseUrl("https://api.twilio.com/2010-04-01")
				.defaultHeaders(h -> h.setBasicAuth(accountSid, authToken))
				.build();
	}

	@Override
	public void send(String phone, String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("From", from);
		form.add("To", "whatsapp:" + phone);
		if (contentSid != null && !contentSid.isBlank()) {
			form.add("ContentSid", contentSid);
			form.add("ContentVariables", toJson(Map.of("1", code)));
		} else {
			form.add("Body", "Mizaj Coffee — votre code de validation : " + code + "\nIl expire dans 10 minutes. Ne le partagez avec personne.");
		}
		try {
			http.post()
					.uri("/Accounts/{sid}/Messages.json", accountSid)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			throw new OtpDeliveryException("Échec de l'envoi WhatsApp via Twilio", e);
		}
	}

	@Override
	public boolean delivers() {
		return true;
	}

	private String toJson(Map<String, String> values) {
		try {
			return json.writeValueAsString(values);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
