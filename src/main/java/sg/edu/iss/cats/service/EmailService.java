package sg.edu.iss.cats.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

//    @Value("${spring.mail.username}")
//    private String senderEmail;
    
    @Value("${spring.mail.password}")
    private String smtpPassword;

//    @PostConstruct
//    public void checkEmailConfiguration(String senderEmail, String) {
//        System.out.println("SMTP Username: " + senderEmail);
//        System.out.println("App Password Length: " + smtpPassword.length());
//    }

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmailWithAttachment(
            String senderEmail,
    		String recipient,
            String subject,
            String message,
            String csvContent) throws Exception {

        MimeMessage email = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(email, true, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(message);

        ByteArrayResource attachment = new ByteArrayResource(
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        helper.addAttachment("AllUsers.csv", attachment, "text/csv");

        mailSender.send(email);
    }
}
