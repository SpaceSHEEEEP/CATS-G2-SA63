package sg.edu.iss.cats.mobile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileDeviceTokenRepository extends JpaRepository<MobileDeviceToken, Long> {
    Optional<MobileDeviceToken> findByTokenHash(String hash);
    List<MobileDeviceToken> findByUser_UserIdOrderByIssuedAtDesc(Integer id);
}
