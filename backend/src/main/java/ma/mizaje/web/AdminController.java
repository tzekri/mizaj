package ma.mizaje.web;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import ma.mizaje.security.Principals;
import ma.mizaje.service.AdminService;
import ma.mizaje.service.DashboardService;
import ma.mizaje.service.ExportService;
import ma.mizaje.service.PlanService;
import ma.mizaje.web.dto.DashboardDtos.Dashboard;
import ma.mizaje.web.dto.Dtos.CreateStaffRequest;
import ma.mizaje.web.dto.Dtos.PlanRequest;
import ma.mizaje.web.dto.Dtos.PlanView;
import ma.mizaje.web.dto.Dtos.StaffView;
import ma.mizaje.web.dto.Dtos.StatsView;
import ma.mizaje.web.dto.Dtos.SubscriberView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final AdminService admin;
	private final PlanService plans;
	private final DashboardService dashboard;
	private final ExportService exports;

	public AdminController(AdminService admin, PlanService plans, DashboardService dashboard, ExportService exports) {
		this.admin = admin;
		this.plans = plans;
		this.dashboard = dashboard;
		this.exports = exports;
	}

	/** Tableau de bord : from / to au format AAAA-MM-JJ (défaut : 30 derniers jours), shop optionnel. */
	@GetMapping("/dashboard")
	Dashboard dashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) String shop) {
		return dashboard.dashboard(from, to, shop);
	}

	@GetMapping("/export/consommations.csv")
	ResponseEntity<byte[]> exportRedemptions(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) String shop) {
		return csv("kidima-consommations-" + from + "-" + to + ".csv", exports.redemptions(from, to, shop));
	}

	@GetMapping("/export/abonnements.csv")
	ResponseEntity<byte[]> exportSubscriptions() {
		return csv("kidima-abonnements.csv", exports.subscriptions());
	}

	private static ResponseEntity<byte[]> csv(String filename, String content) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.body(content.getBytes(StandardCharsets.UTF_8));
	}

	@GetMapping("/plans")
	List<PlanView> plans() {
		return plans.allPlans();
	}

	@PostMapping("/plans")
	PlanView createPlan(@Valid @RequestBody PlanRequest req) {
		return plans.create(req);
	}

	@PutMapping("/plans/{code}")
	PlanView updatePlan(@PathVariable String code, @Valid @RequestBody PlanRequest req) {
		return plans.update(code, req);
	}

	@GetMapping("/stats")
	StatsView stats() {
		return admin.stats();
	}

	@GetMapping("/subscribers")
	List<SubscriberView> subscribers() {
		return admin.subscribers();
	}

	@GetMapping("/staff")
	List<StaffView> staff() {
		return admin.staff();
	}

	@PostMapping("/staff")
	StaffView createStaff(@Valid @RequestBody CreateStaffRequest req) {
		return admin.createStaff(req);
	}

	@PostMapping("/staff/{id}/active")
	StaffView setActive(@PathVariable long id, @RequestParam boolean value) {
		return admin.setActive(id, value, Principals.currentId());
	}
}
