package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import sg.edu.iss.cats.captcha.CaptchaService;
import sg.edu.iss.cats.controller.AdminController;
import sg.edu.iss.cats.controller.LoginController;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.HolidayRepository;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.security.SecurityConfig;

@WebMvcTest({AdminController.class, LoginController.class})
@Import(SecurityConfig.class)
class LoginCaptchaRenderingTests {
    @Autowired private MockMvc mvc;

    @MockitoBean private UserRepository users;
    @MockitoBean private CourseRepository courses;
    @MockitoBean private HolidayRepository holidays;
    @MockitoBean private CaptchaService captcha;

    @Test
    void adminLoginHasExactlyOneAdminCaptcha() throws Exception {
        assertSingleCaptcha("/admin/login", "login-admin", "login-staff");
    }

    @Test
    void staffLoginHasExactlyOneStaffCaptcha() throws Exception {
        assertSingleCaptcha("/staff/login", "login-staff", "login-admin");
    }

    private void assertSingleCaptcha(String path, String expected, String unexpected) throws Exception {
        String html = mvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("data-captcha-scope=\"" + expected + "\"")
                .doesNotContain("data-captcha-scope=\"" + unexpected + "\"");
        assertThat(html.split("data-captcha-scope=", -1)).hasSize(2);
        assertThat(html.split("name=\"captchaId\"", -1)).hasSize(2);
        assertThat(html.split("name=\"captchaAnswer\"", -1)).hasSize(2);
    }
}
