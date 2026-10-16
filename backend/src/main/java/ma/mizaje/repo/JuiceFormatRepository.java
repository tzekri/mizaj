package ma.mizaje.repo;

import ma.mizaje.domain.JuiceFormat;
import ma.mizaje.domain.JuiceSize;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuiceFormatRepository extends JpaRepository<JuiceFormat, JuiceSize> {
}
