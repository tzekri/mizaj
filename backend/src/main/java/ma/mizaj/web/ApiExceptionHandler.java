package ma.mizaj.web;

import jakarta.persistence.OptimisticLockException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<Map<String, Object>> handleApi(ApiException e) {
		return body(e.getStatus(), e.getCode(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(f -> f.getField() + " : " + f.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return body(HttpStatus.BAD_REQUEST, null, message);
	}

	@ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
	ResponseEntity<Map<String, Object>> handleConcurrent(Exception e) {
		return body(HttpStatus.CONFLICT, null, "Cette carte vient d'être modifiée sur une autre caisse, scannez-la à nouveau.");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<Map<String, Object>> handleDenied(AccessDeniedException e) {
		return body(HttpStatus.FORBIDDEN, null, "Accès refusé.");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException e) {
		return body(HttpStatus.NOT_FOUND, null, "Ressource introuvable.");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Map<String, Object>> handleOther(Exception e) {
		log.error("Erreur inattendue", e);
		return body(HttpStatus.INTERNAL_SERVER_ERROR, null, "Erreur interne, réessayez plus tard.");
	}

	private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", status.value());
		if (code != null) {
			body.put("code", code);
		}
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}
