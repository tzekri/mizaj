package ma.mizaje.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.JuiceCategory;
import ma.mizaje.domain.JuiceIngredient;
import ma.mizaje.domain.Plan;
import ma.mizaje.repo.AppSettingRepository;
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
		new DataSeeder(mock(StaffUserRepository.class), plans, mock(JuiceIngredientRepository.class), mock(PasswordEncoder.class), mock(AppSettingRepository.class),
				"admin@kidima.ma", "x", false).updateFormerPlanPrices();
		assertEquals(50, decouverte.getPrice());
		assertEquals(125, espresso.getPrice());
		assertEquals(210, lait.getPrice());
	}

	@Test
	void juiceComposerBecomesSmoothiesWithThreeBases() {
		JuiceIngredientRepository ingredients = mock(JuiceIngredientRepository.class);
		JuiceIngredient orange = new JuiceIngredient("orange", "Orange pressée", JuiceCategory.BASE, 0, "#f7a21b", 0);
		JuiceIngredient carotte = new JuiceIngredient("carotte", "Carotte", JuiceCategory.BASE, 2, "#ef8a2a", 1);
		JuiceIngredient fraise = new JuiceIngredient("fraise", "Fraise", JuiceCategory.FRUIT, 5, "#e5414f", 2);
		when(ingredients.findByCode("orange")).thenReturn(Optional.of(orange));
		when(ingredients.findByCode("yaourt")).thenReturn(Optional.empty());
		when(ingredients.findByCode("lait-amande")).thenReturn(Optional.empty());
		List<JuiceIngredient> created = new ArrayList<>();
		when(ingredients.save(any(JuiceIngredient.class))).thenAnswer(i -> {
			created.add(i.getArgument(0));
			return i.getArgument(0);
		});
		when(ingredients.findAll()).thenReturn(List.of(orange, carotte, fraise));
		new DataSeeder(mock(StaffUserRepository.class), mock(PlanRepository.class), ingredients, mock(PasswordEncoder.class),
				mock(AppSettingRepository.class), "admin@kidima.ma", "x", false).switchToSmoothieBases();
		assertEquals(List.of("Yaourt nature", "Lait d'amande"), created.stream().map(JuiceIngredient::getName).toList());
		assertEquals("Jus d'orange", orange.getName());
		assertTrue(orange.isAvailable());
		assertFalse(carotte.isAvailable()); // ancienne base masquée
		assertTrue(fraise.isAvailable()); // les fruits restent
	}
}
