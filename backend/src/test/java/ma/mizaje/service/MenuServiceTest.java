package ma.mizaje.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.MenuCategory;
import ma.mizaje.domain.MenuItem;
import ma.mizaje.domain.MenuTone;
import ma.mizaje.repo.MenuCategoryRepository;
import ma.mizaje.repo.MenuItemRepository;
import ma.mizaje.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MenuServiceTest {

	private final MenuCategoryRepository categories = mock(MenuCategoryRepository.class);
	private final MenuItemRepository items = mock(MenuItemRepository.class);
	private final MenuService service = new MenuService(categories, items);
	private MenuCategory coffee;
	private MenuCategory hidden;
	private MenuItem espresso;
	private MenuItem latte;
	private MenuItem soldOut;

	@BeforeEach
	void setUp() {
		coffee = category(1, "coffee", true);
		hidden = category(2, "secret", false);
		espresso = item(10, coffee, "Espresso", 0, true);
		latte = item(11, coffee, "Latte", 1, true);
		soldOut = item(12, coffee, "Cold Brew", 2, false);
		when(categories.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(coffee, hidden));
		when(items.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(espresso, latte, soldOut, item(13, hidden, "X", 0, true)));
		when(items.findByCategoryOrderBySortOrderAscIdAsc(coffee)).thenReturn(List.of(espresso, latte, soldOut));
		when(items.findById(11L)).thenReturn(Optional.of(latte));
		when(categories.findById(1L)).thenReturn(Optional.of(coffee));
		when(items.countByCategory(coffee)).thenReturn(3L);
	}

	@Test
	void publicMenuHidesHiddenCategoriesAndSoldOutItems() {
		var menu = service.publicMenu();
		assertEquals(1, menu.size());
		assertEquals(List.of("Espresso", "Latte"), menu.get(0).items().stream().map(i -> i.name()).toList());
		assertEquals(3, service.adminMenu().get(0).items().size());
	}

	@Test
	void moveSwapsWithNeighbourAndRenumbers() {
		service.moveItem(11, true);
		assertEquals(1, espresso.getSortOrder());
		assertEquals(0, latte.getSortOrder());
		assertEquals(2, soldOut.getSortOrder());
	}

	@Test
	void categoryWithItemsCannotBeDeleted() {
		assertThrows(ApiException.class, () -> service.deleteCategory(1));
	}

	private static MenuCategory category(long id, String slug, boolean visible) {
		MenuCategory c = new MenuCategory(slug, (int) id);
		c.update("Hot", "Coffee", null, MenuTone.CREAM, "1558220829-4694a46bb01f", visible);
		ReflectionTestUtils.setField(c, "id", id);
		return c;
	}

	private static MenuItem item(long id, MenuCategory c, String name, int order, boolean available) {
		MenuItem i = new MenuItem(c, order);
		i.update(name, null, 20, null, null, "1558220829-4694a46bb01f", false, null, available);
		ReflectionTestUtils.setField(i, "id", id);
		return i;
	}
}
