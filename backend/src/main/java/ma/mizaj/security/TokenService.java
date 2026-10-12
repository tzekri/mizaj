package ma.mizaj.security;

import java.time.Duration;
import java.time.Instant;
import ma.mizaj.domain.Customer;
import ma.mizaj.domain.StaffUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Émet les jetons : courts pour le personnel, longs pour les clients (qui restent connectés sur leur téléphone). */
@Service
public class TokenService {

	public static final String ROLE_CUSTOMER = "CUSTOMER";

	private final JwtEncoder encoder;
	private final Duration staffValidity;
	private final Duration customerValidity;

	public TokenService(JwtEncoder encoder,
			@Value("${app.jwt.staff-validity-hours}") long staffHours,
			@Value("${app.jwt.customer-validity-days}") long customerDays) {
		this.encoder = encoder;
		this.staffValidity = Duration.ofHours(staffHours);
		this.customerValidity = Duration.ofDays(customerDays);
	}

	public String issue(StaffUser user) {
		return encode(JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.expiresAt(Instant.now().plus(staffValidity))
				.claim("role", user.getRole().name())
				.claim("name", user.getFullName()));
	}

	public String issue(Customer customer) {
		return encode(JwtClaimsSet.builder()
				.subject(customer.getId().toString())
				.expiresAt(Instant.now().plus(customerValidity))
				.claim("role", ROLE_CUSTOMER));
	}

	private String encode(JwtClaimsSet.Builder claims) {
		claims.issuer("mizaj").issuedAt(Instant.now());
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}
}
