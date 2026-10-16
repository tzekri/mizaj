package ma.mizaje.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlanServiceTest {

	@Test
	void slugifiesPlanNames() {
		assertEquals("lait-douceur", PlanService.slug("Lait & Douceur"));
		assertEquals("decouverte", PlanService.slug("Découverte"));
		assertEquals("formule", PlanService.slug("—"));
	}
}
