package sg.edu.iss.cats.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Durable mail notification, committed with the application status change. */
@Entity @Getter @Setter
@Table(name = "notification_outbox")
public class NotificationOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false, length = 4000)
    private String body;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;
    private LocalDateTime sentAt;
    @Column(nullable = false)
    private int attempts;
    private String lastError;
}
