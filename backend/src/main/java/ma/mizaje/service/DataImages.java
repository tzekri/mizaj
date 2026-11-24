package ma.mizaje.service;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ma.mizaje.web.ApiException;

/** Images envoyées par l'administration en « data URL » (JPEG, PNG ou WebP), gardées en base. */
final class DataImages {

	static final int MAX_BYTES = 2_500_000;
	private static final Pattern DATA_URL = Pattern.compile("^data:(image/(?:jpeg|png|webp));base64,([A-Za-z0-9+/=\\s]+)$");

	/** Image décodée : type (ex. image/jpeg) et contenu. */
	record Decoded(String type, byte[] data) {
	}

	private DataImages() {
	}

	static Decoded decode(String dataUrl) {
		Matcher m = DATA_URL.matcher(dataUrl);
		if (!m.matches()) {
			throw ApiException.badRequest("Image non reconnue : utilisez une photo JPEG, PNG ou WebP.");
		}
		byte[] data;
		try {
			data = Base64.getMimeDecoder().decode(m.group(2));
		} catch (IllegalArgumentException e) {
			throw ApiException.badRequest("Image illisible.");
		}
		if (data.length > MAX_BYTES) {
			throw ApiException.badRequest("Image trop lourde (2,5 Mo au maximum).");
		}
		return new Decoded(m.group(1), data);
	}
}
