package ma.mizaj.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** Code de validation envoyé par WhatsApp. Seule l'empreinte du code est stockée. */
@Entity
@Table(name = "otp_challenges", indexes = @Index(columnList = "phone, createdAt"))
public class OtpChallenge {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String phone;

	@Column(nullable = false, length = 64)
	private String codeHash;

	/** Prénom saisi à l'inscription, appliqué au compte une fois le code validé. */
	@Column(length = 60)
	private String firstName;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private int attempts;

	private Instant usedAt;

	protected OtpChallenge() {
	}

	public OtpChallenge(String phone, String codeHash, String firstName, Instant createdAt, Instant expiresAt) {
		this.phone = phone;
		this.codeHash = codeHash;
		this.firstName = firstName;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public void recordAttempt() { attempts++; }
	public void markUsed(Instant now) { usedAt = now; }

	public Long getId() { return id; }
	public String getPhone() { return phone; }
	public String getCodeHash() { return codeHash; }
	public String getFirstName() { return firstName; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public int getAttempts() { return attempts; }
	public Instant getUsedAt() { return usedAt; }
}
