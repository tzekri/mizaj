package ma.mizaje.repo;

import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.Customer;
import ma.mizaje.domain.JuiceFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuiceFavoriteRepository extends JpaRepository<JuiceFavorite, Long> {

	List<JuiceFavorite> findByCustomerOrderByCreatedAtDesc(Customer customer);

	long countByCustomer(Customer customer);

	Optional<JuiceFavorite> findByIdAndCustomer(Long id, Customer customer);
}
