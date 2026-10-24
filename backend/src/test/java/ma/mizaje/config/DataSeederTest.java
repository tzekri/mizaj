package ma.mizaje.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import ma.mizaje.domain.Plan;
import ma.mizaje.repo.JuiceIngredientRepository;
import ma.mizaje.repo.PlanRepository;
import ma.mizaje.repo.StaffUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class DataSeederTest {

	@Test
	void newPlanPricesOnlyReplaceTheFormerOnes() {
		PlanRepository plans = mock(PlanRepository.class);
		Plan decouverte = new Plan("decouverte", "Découverte", null, 45, 10, 15, "espresso", "espressos", 1);
		Plan espresso = new Plan("espresso", "Espresso", null, 125, 30, 30, "espresso", "espressos", 2); // déjà changé par l'admin
		when(plans.findByCode("decouverte")).thenReturn(Optional.of(decouverte));
		Plan lait = new Plan("lait-douceur", "Lait & Douceur", null, 220, 30, 30, "boisson au lait", "boissons au lait", 3);
		when(plans.findByCode("espresso")).thenReturn(Optional.of(espresso));
		when(plans.findByCode("lait-douceur")).thenReturn(Optional.of(lait));
		new DataSeeder(mock(StaffUserRepository.class), plans, mock(JuiceIngredientRepository.class), mock(PasswordEncoder.class),
				"admin@kidima.ma", "x", false).updateFormerPlanPrices();
		assertEquals(50, decouverte.getPrice());
		assertEquals(125, espresso.getPrice());
		assertEquals(210, lait.getPrice());
	}
}
