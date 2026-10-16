package ma.mizaje.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import ma.mizaje.domain.JuiceCategory;
import ma.mizaje.domain.JuiceIngredient;
import ma.mizaje.domain.JuiceSize;
import ma.mizaje.repo.CustomerRepository;
import ma.mizaje.repo.JuiceFavoriteRepository;
import ma.mizaje.repo.JuiceIngredientRepository;
import ma.mizaje.repo.JuiceOrderRepository;
import ma.mizaje.repo.StaffUserRepository;
import ma.mizaje.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JuiceServiceTest {

	private JuiceService service;
	private final JuiceIngredient mangoOff = new JuiceIngredient("mangue", "Mangue", JuiceCategory.FRUIT, 6, "#f9a825", 4);

	@BeforeEach
	void setUp() {
		JuiceIngredientRepository ingredients = mock(JuiceIngredientRepository.class);
		mangoOff.update("Mangue", JuiceCategory.FRUIT, 6, "#f9a825", false);
		when(ingredients.findAll()).thenReturn(List.of(
				new JuiceIngredient("orange", "Orange pressée", JuiceCategory.BASE, 0, "#f7a21b", 1),
				new JuiceIngredient("pomme", "Pomme", JuiceCategory.BASE, 3, "#c9d65a", 2),
				new JuiceIngredient("fraise", "Fraise", JuiceCategory.FRUIT, 5, "#e5414f", 3),
				mangoOff,
				new JuiceIngredient("banane", "Banane", JuiceCategory.FRUIT, 4, "#f2e19c", 5),
				new JuiceIngredient("kiwi", "Kiwi", JuiceCategory.FRUIT, 5, "#7cb342", 6),
				new JuiceIngredient("menthe", "Menthe", JuiceCategory.EXTRA, 2, "#3e9b5b", 7)));
		service = new JuiceService(ingredients, mock(JuiceOrderRepository.class), mock(JuiceFavoriteRepository.class),
				mock(CustomerRepository.class), mock(StaffUserRepository.class), Clock.systemUTC(), ZoneId.of("Africa/Casablanca"));
	}

	@Test
	void pricesBySize() {
		// 33 cl : 15 + 0 + 5 + 2 = 22 DH ; 50 cl : 20 + round(7 × 1,4) = 30 DH
		assertEquals(22, service.resolve(List.of("menthe", "fraise", "orange"), JuiceSize.S33, null, true).price());
		assertEquals(30, service.resolve(List.of("orange", "fraise", "menthe"), JuiceSize.S50, null, true).price());
	}

	@Test
	void ordersIngredientsBaseFirstAndBuildsAStableSignature() {
		var a = service.resolve(List.of("menthe", "fraise", "orange"), JuiceSize.S33, " Matin ", true);
		var b = service.resolve(List.of("orange", "menthe", "fraise"), JuiceSize.S33, null, true);
		assertEquals("Orange pressée · Fraise · Menthe", a.label());
		assertEquals("orange|fraise|menthe", a.codes());
		assertEquals("Matin", a.name());
		assertEquals(a.signature(), b.signature());
	}

	@Test
	void enforcesCompositionRules() {
		assertThrows(ApiException.class, () -> service.resolve(List.of("fraise"), JuiceSize.S33, null, true)); // pas de base
		assertThrows(ApiException.class, () -> service.resolve(List.of("orange", "pomme"), JuiceSize.S33, null, true)); // 2 bases
		assertThrows(ApiException.class, () -> service.resolve(List.of("orange", "fraise", "banane", "kiwi"), JuiceSize.S33, null, true));
		assertThrows(ApiException.class, () -> service.resolve(List.of("orange", "inconnu"), JuiceSize.S33, null, true));
		assertThrows(ApiException.class, () -> service.resolve(List.of("orange"), null, null, true));
	}

	@Test
	void unavailableIngredientBlocksAnOrderButNotAFavoriteDisplay() {
		assertThrows(ApiException.class, () -> service.resolve(List.of("orange", "mangue"), JuiceSize.S33, null, true));
		assertFalse(service.resolve(List.of("orange", "mangue"), JuiceSize.S33, null, false).available());
	}
}
