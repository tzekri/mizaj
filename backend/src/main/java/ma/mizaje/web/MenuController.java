package ma.mizaje.web;

import jakarta.validation.Valid;
import java.util.List;
import ma.mizaje.service.MenuService;
import ma.mizaje.web.dto.MenuDtos.CategoryRequest;
import ma.mizaje.web.dto.MenuDtos.ItemRequest;
import ma.mizaje.web.dto.MenuDtos.MenuCategoryView;
import ma.mizaje.web.dto.MenuDtos.MenuItemView;
import ma.mizaje.web.dto.MenuDtos.MoveRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Menu du coffee shop : lecture publique, gestion par l'administrateur. */
@RestController
public class MenuController {

	private final MenuService menu;

	public MenuController(MenuService menu) {
		this.menu = menu;
	}

	@GetMapping("/api/public/menu")
	List<MenuCategoryView> publicMenu() {
		return menu.publicMenu();
	}

	// --- Administration ---

	@GetMapping("/api/admin/menu")
	List<MenuCategoryView> adminMenu() {
		return menu.adminMenu();
	}

	@PostMapping("/api/admin/menu/categories")
	MenuCategoryView createCategory(@Valid @RequestBody CategoryRequest req) {
		return menu.createCategory(req);
	}

	@PutMapping("/api/admin/menu/categories/{id}")
	MenuCategoryView updateCategory(@PathVariable long id, @Valid @RequestBody CategoryRequest req) {
		return menu.updateCategory(id, req);
	}

	@PostMapping("/api/admin/menu/categories/{id}/move")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void moveCategory(@PathVariable long id, @Valid @RequestBody MoveRequest req) {
		menu.moveCategory(id, "up".equals(req.direction()));
	}

	@DeleteMapping("/api/admin/menu/categories/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteCategory(@PathVariable long id) {
		menu.deleteCategory(id);
	}

	@PostMapping("/api/admin/menu/items")
	MenuItemView createItem(@Valid @RequestBody ItemRequest req) {
		return menu.createItem(req);
	}

	@PutMapping("/api/admin/menu/items/{id}")
	MenuItemView updateItem(@PathVariable long id, @Valid @RequestBody ItemRequest req) {
		return menu.updateItem(id, req);
	}

	@PostMapping("/api/admin/menu/items/{id}/move")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void moveItem(@PathVariable long id, @Valid @RequestBody MoveRequest req) {
		menu.moveItem(id, "up".equals(req.direction()));
	}

	@DeleteMapping("/api/admin/menu/items/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteItem(@PathVariable long id) {
		menu.deleteItem(id);
	}
}
