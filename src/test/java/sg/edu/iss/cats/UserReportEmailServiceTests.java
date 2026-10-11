package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import sg.edu.iss.cats.service.UserCsvService;
import sg.edu.iss.cats.service.UserReportEmailService;

class UserReportEmailServiceTests {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final UserCsvService csvService = mock(UserCsvService.class);

    @Test
    void usesConfiguredSenderAndAttachesTheCsvBytes() throws Exception {
        byte[] bytes = "userId,name\r\n1,\"Employee\"\r\n".getBytes(StandardCharsets.UTF_8);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(csvService.generateCsv()).thenReturn(bytes);

        new UserReportEmailService(mailSender, csvService, "reports@example.org")
                .sendReport("recipient@example.org");

        verify(mailSender).send(message);
        assertThat(message.getFrom()[0].toString()).isEqualTo("reports@example.org");
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
                .isEqualTo("recipient@example.org");
        assertThat(message.getSubject()).isEqualTo("CATS - User Report");
        Multipart multipart = (Multipart) message.getContent();
        assertThat(multipart.getCount()).isEqualTo(2);
        assertThat(multipart.getBodyPart(1).getFileName()).isEqualTo("AllUsers.csv");
        assertThat(multipart.getBodyPart(1).getInputStream().readAllBytes()).isEqualTo(bytes);
    }

    @Test
    void refusesToSendWhenSenderIsNotConfigured() {
        UserReportEmailService service = new UserReportEmailService(mailSender, csvService, " ");
        assertThatThrownBy(() -> service.sendReport("recipient@example.org"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mailSender, csvService);
    }
}
