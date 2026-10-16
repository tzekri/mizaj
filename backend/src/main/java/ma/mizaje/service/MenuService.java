package ma.mizaje.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import ma.mizaje.domain.MenuCategory;
import ma.mizaje.domain.MenuItem;
import ma.mizaje.repo.MenuCategoryRepository;
import ma.mizaje.repo.MenuItemRepository;
import ma.mizaje.web.ApiException;
import ma.mizaje.web.dto.MenuDtos.CategoryRequest;
import ma.mizaje.web.dto.MenuDtos.ItemRequest;
import ma.mizaje.web.dto.MenuDtos.MenuCategoryView;
import ma.mizaje.web.dto.MenuDtos.MenuItemView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Menu du coffee shop : affichage public et gestion par l'administrateur (prix, produits, catégories, ordre). */
@Service
public class MenuService {

	private final MenuCategoryRepository categories;
	private final MenuItemRepository items;

	public MenuService(MenuCategoryRepository categories, MenuItemRepository items) {
		this.categories = categories;
		this.items = items;
	}

	/** Menu public : catégories visibles et produits disponibles ; une catégorie sans produit disponible est masquée. */
	@Transactional(readOnly = true)
	public List<MenuCategoryView> publicMenu() {
		return menu(false).stream().filter(c -> c.visible() && !c.items().isEmpty()).toList();
	}

	/** Menu complet pour l'administration, y compris les catégories masquées et les produits en rupture. */
	@Transactional(readOnly = true)
	public List<MenuCategoryView> adminMenu() {
		return menu(true);
	}

	private List<MenuCategoryView> menu(boolean all) {
		Map<Long, List<MenuItemView>> byCategory = items.findAllByOrderBySortOrderAscIdAsc().stream()
				.filter(i -> all || i.isAvailable())
				.map(MenuItemView::of)
				.collect(Collectors.groupingBy(MenuItemView::categoryId));
		return categories.findAllByOrderBySortOrderAscIdAsc().stream()
				.map(c -> MenuCategoryView.of(c, byCategory.getOrDefault(c.getId(), List.of())))
				.toList();
	}

	// --- Catégories ---

	@Transactional
	public MenuCategoryView createCategory(CategoryRequest req) {
		String base = PlanService.slug(req.light() + " " + req.bold());
		String slug = base;
		for (int i = 2; categories.existsBySlug(slug); i++) {
			slug = base + "-" + i;
		}
		MenuCategory c = new MenuCategory(slug, (int) categories.count());
		apply(c, req);
		return MenuCategoryView.of(categories.save(c), List.of());
	}

	/** L'adresse de la page (/menu/slug) ne change pas, pour garder les liens partagés valides. */
	@Transactional
	public MenuCategoryView updateCategory(long id, CategoryRequest req) {
		MenuCategory c = category(id);
		apply(c, req);
		return MenuCategoryView.of(c, items.findByCategoryOrderBySortOrderAscIdAsc(c).stream().map(MenuItemView::of).toList());
	}

	@Transactional
	public void moveCategory(long id, boolean up) {
		move(categories.findAllByOrderBySortOrderAscIdAsc(), category(id), up, MenuCategory::setSortOrder);
	}

	@Transactional
	public void deleteCategory(long id) {
		MenuCategory c = category(id);
		if (items.countByCategory(c) > 0) {
			throw ApiException.conflict("NOT_EMPTY", "Retirez ou déplacez d'abord les produits de cette catégorie.");
		}
		categories.delete(c);
	}

	private static void apply(MenuCategory c, CategoryRequest req) {
		c.update(req.light().trim(), req.bold().trim(), blankToNull(req.arabic()), req.tone(), req.photo().trim(), req.visible());
	}

	private MenuCategory category(long id) {
		return categories.findById(id).orElseThrow(() -> ApiException.notFound("Catégorie introuvable."));
	}

	// --- Produits ---

	@Transactional
	public MenuItemView createItem(ItemRequest req) {
		MenuCategory c = category(req.categoryId());
		MenuItem item = new MenuItem(c, (int) items.countByCategory(c));
		apply(item, req);
		return MenuItemView.of(items.save(item));
	}

	/** Un changement de catégorie place le produit à la fin de sa nouvelle catégorie. */
	@Transactional
	public MenuItemView updateItem(long id, ItemRequest req) {
		MenuItem item = item(id);
		if (!item.getCategory().getId().equals(req.categoryId())) {
			MenuCategory target = category(req.categoryId());
			item.setSortOrder((int) items.countByCategory(target));
			item.setCategory(target);
		}
		apply(item, req);
		return MenuItemView.of(item);
	}

	@Transactional
	public void moveItem(long id, boolean up) {
		MenuItem item = item(id);
		move(items.findByCategoryOrderBySortOrderAscIdAsc(item.getCategory()), item, up, MenuItem::setSortOrder);
	}

	@Transactional
	public void deleteItem(long id) {
		items.delete(item(id));
	}

	private static void apply(MenuItem item, ItemRequest req) {
		item.update(req.name().trim(), blankToNull(req.description()), req.price(), req.intensity(), blankToNull(req.volume()),
				req.photo().trim(), req.featured(), blankToNull(req.tag()), req.available());
	}

	private MenuItem item(long id) {
		return items.findById(id).orElseThrow(() -> ApiException.notFound("Produit introuvable."));
	}

	// --- Ordre ---

	/** Échange l'élément avec son voisin, puis renumérote la liste (0, 1, 2…). */
	private static <T> void move(List<T> ordered, T element, boolean up, BiConsumer<T, Integer> setOrder) {
		List<T> list = new ArrayList<>(ordered);
		int i = list.indexOf(element);
		int j = up ? i - 1 : i + 1;
		if (i >= 0 && j >= 0 && j < list.size()) {
			list.set(i, list.get(j));
			list.set(j, element);
		}
		for (int k = 0; k < list.size(); k++) {
			setOrder.accept(list.get(k), k);
		}
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
