package sg.edu.iss.cats.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Emails the same report available through the admin-only download endpoint.
 * The configured SMTP account is the sender; a signed-in user's address is not trusted for From.
 */
@Service
public class UserReportEmailService {
    private final JavaMailSender mailSender;
    private final UserCsvService csvService;
    private final String senderAddress;

    public UserReportEmailService(JavaMailSender mailSender, UserCsvService csvService,
            @Value("${spring.mail.username:}") String senderAddress) {
        this.mailSender = mailSender;
        this.csvService = csvService;
        this.senderAddress = senderAddress;
    }

    public void sendReport(String recipient) throws MessagingException {
        if (senderAddress == null || senderAddress.isBlank()) {
            throw new IllegalStateException("SMTP sender is not configured");
        }

        byte[] csv = csvService.generateCsv();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(senderAddress);
        helper.setTo(recipient);
        helper.setSubject("CATS - User Report");
        helper.setText("Please find attached the latest CATS user report.");
        helper.addAttachment("AllUsers.csv", new ByteArrayResource(csv), "text/csv; charset=UTF-8");

        mailSender.send(message);
    }
}
