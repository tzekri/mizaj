package ma.mizaj.web;

import jakarta.validation.Valid;
import java.util.List;
import ma.mizaj.security.Principals;
import ma.mizaj.service.AdminService;
import ma.mizaj.service.PlanService;
import ma.mizaj.web.dto.Dtos.CreateStaffRequest;
import ma.mizaj.web.dto.Dtos.PlanRequest;
import ma.mizaj.web.dto.Dtos.PlanView;
import ma.mizaj.web.dto.Dtos.StaffView;
import ma.mizaj.web.dto.Dtos.StatsView;
import ma.mizaj.web.dto.Dtos.SubscriberView;
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

	public AdminController(AdminService admin, PlanService plans) {
		this.admin = admin;
		this.plans = plans;
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
