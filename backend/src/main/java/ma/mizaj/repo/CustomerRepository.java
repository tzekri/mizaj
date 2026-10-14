package ma.mizaj.repo;

import java.time.Instant;
import java.util.Optional;
import ma.mizaj.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByPhone(String phone);

	Optional<Customer> findByQrToken(String qrToken);

	boolean existsByQrToken(String qrToken);

	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
}
