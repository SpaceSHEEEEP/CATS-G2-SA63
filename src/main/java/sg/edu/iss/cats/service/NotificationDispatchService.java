package sg.edu.iss.cats.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.repository.NotificationOutboxRepository;

/** SMTP is outside the application transaction; email attempts are retried after failure.
 * At-least-once delivery means rare duplicates are possible following a crash after sending.
 * A deployment should configure PUBLIC_BASE_URL, SMTP_HOST and sender credentials. */
@Service
public class NotificationDispatchService {
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);
    private final NotificationOutboxRepository outbox;
    private final ObjectProvider<JavaMailSender> mailProvider;
    private final String baseUrl;
    private final String from;

    public NotificationDispatchService(NotificationOutboxRepository outbox,
            ObjectProvider<JavaMailSender> mailProvider,
            @Value("${cats.public-base-url:}") String baseUrl,
            @Value("${spring.mail.username:}") String from) {
        this.outbox = outbox; this.mailProvider = mailProvider;
        this.baseUrl = baseUrl; this.from = from;
    }

    private String loginUrl() {
        if (baseUrl == null || baseUrl.isBlank()) return null;
        URI url = URI.create(baseUrl.strip());
        if (!("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme()))
                || url.getHost() == null || url.getUserInfo() != null
                || url.getQuery() != null || url.getFragment() != null)
            throw new IllegalStateException("PUBLIC_BASE_URL must be an http(s) public application origin");
        return baseUrl.strip().replaceAll("/+$", "") + "/staff/login";
    }

    @Scheduled(fixedDelayString = "${cats.mail.dispatch-delay-ms:60000}")
    @Transactional
    public void dispatchDue() {
        JavaMailSender sender = mailProvider.getIfAvailable();
        if (sender == null || from == null || from.isBlank()) return;
        String loginLink = loginUrl();
        if (loginLink == null) return; // Preserve queued emails until public URL is configured.
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Singapore"));
        for (var message : outbox.findDueForDispatch(now, PageRequest.of(0, 20))) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(from); mail.setTo(message.getRecipient());
                mail.setSubject(message.getSubject());
                mail.setText(message.getBody() + "\n\nLog in: " + loginLink + "\n");
                sender.send(mail);
                message.setSentAt(LocalDateTime.now(ZoneId.of("Asia/Singapore")));
                message.setLastError(null);
            } catch (RuntimeException e) {
                message.setAttempts(message.getAttempts() + 1);
                int seconds = (int) Math.min(3600L, 30L << Math.min(7, message.getAttempts()));
                message.setNextAttemptAt(now.plusSeconds(seconds));
                message.setLastError(e.getClass().getSimpleName()); // Do not store SMTP credentials/error internals.
                log.warn("CATS notification #{} delivery failed; will retry", message.getId());
            }
            outbox.save(message);
        }
    }
}
