package ma.mizaje.security;

import ma.mizaje.web.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Identifiant (client ou membre du personnel) porté par le jeton de la requête en cours. */
public final class Principals {

	private Principals() {
	}

	public static long currentId() {
		if (!(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt jwt)) {
			throw ApiException.forbidden();
		}
		return Long.parseLong(jwt.getSubject());
	}
}
