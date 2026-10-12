package sg.edu.iss.cats.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import sg.edu.iss.cats.model.NotificationOutbox;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificationOutbox n WHERE n.sentAt IS NULL " +
           "AND n.nextAttemptAt <= :now ORDER BY n.id")
    List<NotificationOutbox> findDueForDispatch(@Param("now") LocalDateTime now, Pageable limit);
}
