package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_users")
public class StaffUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String fullName;

	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StaffRole role;

	/** Coffee shop de rattachement (affiché dans l'historique des validations). */
	@Column(length = 60)
	private String shop;

	@Column(nullable = false)
	private boolean active = true;

	protected StaffUser() {
	}

	public StaffUser(String fullName, String email, String passwordHash, StaffRole role, String shop) {
		this.fullName = fullName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.shop = shop;
	}

	public Long getId() { return id; }
	public String getFullName() { return fullName; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getPasswordHash() { return passwordHash; }
	public StaffRole getRole() { return role; }
	public String getShop() { return shop; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
}
