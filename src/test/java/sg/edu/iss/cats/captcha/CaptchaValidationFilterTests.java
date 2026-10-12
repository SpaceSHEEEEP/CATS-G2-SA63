package sg.edu.iss.cats.captcha;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CaptchaValidationFilterTests {
    @Test void directLoginAndSensitivePostsCannotBypassCaptcha() throws Exception {
        var service = mock(CaptchaService.class);
        var filter = new CaptchaValidationFilter(service);
        for (String path : new String[] {"/staff/login", "/admin/login", "/mfa/challenge",
                "/staff/mobile/pair", "/staff/claim", "/admin/users/email"}) {
            var request = new MockHttpServletRequest("POST", path);
            request.setServletPath(path);
            var response = new MockHttpServletResponse();
            FilterChain downstream = mock(FilterChain.class);
            filter.doFilter(request, response, downstream);
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).contains("captchaError=1");
            verifyNoInteractions(downstream);
        }
    }
}
