package ma.mizaje.repo;

import java.util.List;
import ma.mizaje.domain.MenuCategory;
import ma.mizaje.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

	List<MenuItem> findAllByOrderBySortOrderAscIdAsc();

	List<MenuItem> findByCategoryOrderBySortOrderAscIdAsc(MenuCategory category);

	long countByCategory(MenuCategory category);
}
