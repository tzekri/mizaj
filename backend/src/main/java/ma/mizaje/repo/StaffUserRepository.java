package ma.mizaje.repo;

import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

	Optional<StaffUser> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<StaffUser> findAllByOrderByFullNameAsc();
}
