package ma.mizaj.web;

import org.springframework.http.HttpStatus;

/**
 * Erreur métier renvoyée telle quelle au client, avec un message lisible (en français)
 * et, si besoin, un code que le frontend sait interpréter (ex. demander une confirmation).
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String message) {
		this(status, null, message);
	}

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, message);
	}

	public static ApiException badRequest(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message);
	}

	public static ApiException conflict(String code, String message) {
		return new ApiException(HttpStatus.CONFLICT, code, message);
	}

	public static ApiException tooMany(String message) {
		return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, message);
	}

	public static ApiException forbidden() {
		return new ApiException(HttpStatus.FORBIDDEN, "Accès refusé.");
	}
}
