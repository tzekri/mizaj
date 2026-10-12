package ma.mizaj.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import ma.mizaj.domain.StaffRole;
import ma.mizaj.domain.SubscriptionStatus;

/** Corps des requêtes et réponses JSON de l'API. */
public final class Dtos {

	private Dtos() {
	}

	// --- Public ---

	public record PlanView(int price, int drinks, int days) {
	}

	public record OtpStartRequest(@NotBlank String phone, @Size(max = 60) String firstName) {
	}

	/** devCode n'est présent qu'en mode démo (Twilio non branché). */
	public record OtpStartResponse(String phone, boolean newAccount, int expiresInSeconds, int resendInSeconds,
			boolean delivered, String devCode) {
	}

	public record OtpVerifyRequest(@NotBlank String phone, @NotBlank @Pattern(regexp = "\\d{6}") String code) {
	}

	public record CustomerSession(String token, CardView card) {
	}

	// --- Client ---

	public record HistoryEntry(Instant at, String shop) {
	}

	public record CardView(String firstName, String phone, String qrToken, SubscriptionStatus status,
			int drinksLeft, int drinksTotal, Instant activatedAt, Instant expiresAt, PlanView plan,
			List<HistoryEntry> history) {
	}

	public record UpdateProfileRequest(@Size(max = 60) String firstName) {
	}

	// --- Personnel ---

	public record StaffLoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record StaffSession(String token, String name, StaffRole role, String shop) {
	}

	public record StaffCardView(long customerId, String firstName, String phone, SubscriptionStatus status,
			int drinksLeft, int drinksTotal, Instant activatedAt, Instant expiresAt, Instant lastRedemptionAt,
			Long undoableRedemptionId, Instant memberSince, PlanView plan) {
	}

	public record ScanRequest(@NotBlank String token) {
	}

	public record RedeemRequest(boolean force) {
	}

	public record ActivateRequest(boolean confirmReplace) {
	}

	public record TodayView(String shop, long redemptions, long activations) {
	}

	// --- Administration ---

	public record StatsView(long customers, long activeSubscriptions, long revenueThisMonth,
			long activationsThisMonth, long redemptionsToday, long redemptionsThisMonth) {
	}

	public record StaffView(long id, String fullName, String email, StaffRole role, String shop, boolean active) {
	}

	public record CreateStaffRequest(@NotBlank @Size(max = 80) String fullName, @NotBlank @Email String email,
			@NotBlank @Size(min = 8, max = 72) String password, @NotNull StaffRole role, @Size(max = 60) String shop) {
	}

	public record SubscriberView(long customerId, String firstName, String phone, SubscriptionStatus status,
			int drinksLeft, Instant expiresAt) {
	}
}
