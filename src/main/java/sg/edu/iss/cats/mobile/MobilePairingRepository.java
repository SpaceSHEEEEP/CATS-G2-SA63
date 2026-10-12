package sg.edu.iss.cats.mobile;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface MobilePairingRepository extends JpaRepository<MobilePairing, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MobilePairing> findByCodeHash(String hash);
}
