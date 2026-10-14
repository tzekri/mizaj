package ma.mizaj.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import ma.mizaj.domain.Plan;
import ma.mizaj.domain.StaffRole;
import ma.mizaj.domain.SubscriptionStatus;

/** Corps des requêtes et réponses JSON de l'API. */
public final class Dtos {

	private Dtos() {
	}

	// --- Formules ---

	public record PlanView(String code, String name, String description, int price, int drinks, int days, String unit,
			String unitPlural, boolean active) {

		public static PlanView of(Plan p) {
			return new PlanView(p.getCode(), p.getName(), p.getDescription(), p.getPrice(), p.getDrinks(), p.getDays(),
					p.getUnit(), p.getUnitPlural(), p.isActive());
		}
	}

	public record PlanRequest(
			@NotBlank @Size(max = 60) String name,
			@Size(max = 160) String description,
			@Min(1) @Max(100_000) int price,
			@Min(1) @Max(1000) int drinks,
			@Min(1) @Max(366) int days,
			@NotBlank @Size(max = 40) String unit,
			@NotBlank @Size(max = 40) String unitPlural,
			boolean active) {
	}

	// --- Public ---

	/** channel : « sms » si le client demande explicitement le SMS ; sinon canal préféré (WhatsApp). */
	public record OtpStartRequest(@NotBlank String phone, @Size(max = 60) String firstName, @Size(max = 40) String planCode,
			@Size(max = 20) String channel) {
	}

	/**
	 * channel : canal réellement utilisé (whatsapp / sms) ; smsAvailable : le client peut demander le SMS.
	 * devCode n'est présent qu'en mode démo (Twilio non branché).
	 */
	public record OtpStartResponse(String phone, boolean newAccount, int expiresInSeconds, int resendInSeconds,
			boolean delivered, String channel, boolean smsAvailable, String devCode) {
	}

	public record OtpVerifyRequest(@NotBlank String phone, @NotBlank @Pattern(regexp = "\\d{6}") String code) {
	}

	public record CustomerSession(String token, CardView card) {
	}

	// --- Client ---

	public record HistoryEntry(Instant at, String shop) {
	}

	/** plan : formule de la période en cours, sinon celle choisie par le client. */
	public record CardView(String firstName, String phone, String qrToken, SubscriptionStatus status,
			int drinksLeft, int drinksTotal, Instant activatedAt, Instant expiresAt, PlanView plan,
			List<HistoryEntry> history) {
	}

	public record UpdateProfileRequest(@Size(max = 60) String firstName, @Size(max = 40) String planCode) {
	}

	// --- Personnel ---

	public record StaffLoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record StaffSession(String token, String name, StaffRole role, String shop) {
	}

	/** plan : formule de la période en cours ; preferredPlan : formule à proposer à l'activation. */
	public record StaffCardView(long customerId, String firstName, String phone, SubscriptionStatus status,
			int drinksLeft, int drinksTotal, Instant activatedAt, Instant expiresAt, Instant lastRedemptionAt,
			Long undoableRedemptionId, Instant memberSince, PlanView plan, PlanView preferredPlan) {
	}

	public record ScanRequest(@NotBlank String token) {
	}

	public record RedeemRequest(boolean force) {
	}

	public record ActivateRequest(@Size(max = 40) String planCode, boolean confirmReplace) {
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

	public record SubscriberView(long customerId, String firstName, String phone, String planName, SubscriptionStatus status,
			int drinksLeft, Instant expiresAt) {
	}
}
