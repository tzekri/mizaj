package ma.mizaje.repo;

import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.JuiceIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuiceIngredientRepository extends JpaRepository<JuiceIngredient, Long> {

	Optional<JuiceIngredient> findByCode(String code);

	boolean existsByCode(String code);

	List<JuiceIngredient> findAllByOrderByCategoryAscSortOrderAscIdAsc();
}
