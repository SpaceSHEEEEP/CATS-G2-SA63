package sg.edu.iss.cats.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.NotificationOutboxRepository;

/** Called inside the application's write transaction: rejected writes cannot send phantom emails. */
@Service
public class NotificationOutboxService {
    private final NotificationOutboxRepository messages;
    public NotificationOutboxService(NotificationOutboxRepository messages) { this.messages = messages; }
    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Singapore")); }

    private void enqueue(String address, String subject, String body) {
        if (address == null || address.isBlank()) return;
        NotificationOutbox message = new NotificationOutbox();
        message.setRecipient(address.strip());
        message.setSubject(subject);
        message.setBody(body);
        message.setCreatedAt(now()); message.setNextAttemptAt(now());
        messages.save(message);
    }

    public void applicationSubmitted(Application app) {
        User manager = app.getUser().getManager();
        if (manager == null || !manager.isEnabled()) return;
        enqueue(manager.getEmail(), "CATS: course application #" + app.getId() + " requires a decision",
                "A course application is waiting for your review.\n" +
                "Employee: " + app.getUser().getName() + "\n" +
                "Course: " + app.getCourse().getCourseName() + "\n" +
                "Status: " + app.getStatus() + "\n" +
                "Sign in to review it using your manager account.");
    }

    public void applicationDecided(Application app) {
        enqueue(app.getUser().getEmail(), "CATS: course application #" + app.getId() + " " + app.getStatus(),
                "Your course application has been " + app.getStatus().name().toLowerCase() + ".\n" +
                "Course: " + app.getCourse().getCourseName() + "\n" +
                "Manager's reason: " + app.getManagerReason() + "\n" +
                "Sign in to view the complete application and comments.");
    }
}
