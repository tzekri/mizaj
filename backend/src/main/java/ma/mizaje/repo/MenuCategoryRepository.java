package ma.mizaje.repo;

import java.util.List;
import ma.mizaje.domain.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

	boolean existsBySlug(String slug);

	List<MenuCategory> findAllByOrderBySortOrderAscIdAsc();
}
