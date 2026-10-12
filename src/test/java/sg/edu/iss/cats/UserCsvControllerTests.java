package sg.edu.iss.cats;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import sg.edu.iss.cats.controller.UserCsvController;
import sg.edu.iss.cats.security.SecurityConfig;
import sg.edu.iss.cats.captcha.CaptchaService;
import sg.edu.iss.cats.service.UserCsvService;
import sg.edu.iss.cats.service.UserReportEmailService;

@WebMvcTest(UserCsvController.class)
@Import(SecurityConfig.class)
class UserCsvControllerTests {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CaptchaService captchaService;

    @MockitoBean
    private UserCsvService csvService;

    @MockitoBean
    private UserReportEmailService emailService;

    @BeforeEach
    void bypassCaptchaOnlyForTheseCsvControllerTests() {
        lenient().when(captchaService.verify(nullable(jakarta.servlet.http.HttpSession.class), eq("admin-mail"), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void anonymousCannotDownloadUserDirectory() throws Exception {
        mvc.perform(get("/admin/users.csv")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(csvService);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void staffCannotDownloadUserDirectory() throws Exception {
        mvc.perform(get("/admin/users.csv")).andExpect(status().isForbidden());
        verifyNoInteractions(csvService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotEmailUserDirectory() throws Exception {
        mvc.perform(post("/admin/users/email").with(csrf())
                        .param("recipient", "recipient@example.org").param("captchaId", "test-id").param("captchaAnswer", "test"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(emailService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDownloadCsvWithNoStoreHeaders() throws Exception {
        byte[] bytes = "userId,name\r\n".getBytes(StandardCharsets.UTF_8);
        when(csvService.generateCsv()).thenReturn(bytes);
        mvc.perform(get("/admin/users.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"AllUsers.csv\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void csrfIsRequiredToEmailDirectory() throws Exception {
        mvc.perform(post("/admin/users/email").param("recipient", "recipient@example.org").param("captchaId", "test-id").param("captchaAnswer", "test"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(emailService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanEmailOneRecipientAndGetsFeedback() throws Exception {
        mvc.perform(post("/admin/users/email").with(csrf())
                        .param("recipient", "recipient@example.org").param("captchaId", "test-id").param("captchaAnswer", "test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/index"))
                .andExpect(flash().attribute("successmsg", "User report emailed successfully."));
        verify(emailService).sendReport("recipient@example.org");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reportsMailDeliveryFailureWithoutDisclosingDetails() throws Exception {
        doThrow(new MailSendException("SMTP private diagnostics"))
                .when(emailService).sendReport("recipient@example.org");
        mvc.perform(post("/admin/users/email").with(csrf())
                        .param("recipient", "recipient@example.org").param("captchaId", "test-id").param("captchaAnswer", "test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errormsg",
                        "Could not email the user report. Check mail configuration or try again."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsMultipleRecipientAddresses() throws Exception {
        mvc.perform(post("/admin/users/email").with(csrf())
                        .param("recipient", "first@example.org,second@example.org").param("captchaId", "test-id").param("captchaAnswer", "test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errormsg", "Enter a single valid recipient email address."));
        verifyNoInteractions(emailService);
    }
}
