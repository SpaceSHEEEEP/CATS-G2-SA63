package sg.edu.iss.cats.mfa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import sg.edu.iss.cats.model.User;

class MfaGateFilterTests {
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void passwordsAloneCannotEnterStaffPages() throws Exception {
        User user = new User(); user.setUserId(77);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/staff/index");
        req.setServletPath("/staff/index");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new MfaGateFilter().doFilter(req, resp, chain);
        assertThat(resp.getStatus()).isEqualTo(302);
        assertThat(resp.getRedirectedUrl()).isEqualTo("/mfa/continue");
        verifyNoInteractions(chain);
    }

    @Test void installerDoesNotRequireMfaButPersonalPagesStillDo() throws Exception {
        User user = new User(); user.setUserId(77);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
        var publicReq = new MockHttpServletRequest("GET", "/mobile/download");
        publicReq.setServletPath("/mobile/download");
        var chain = mock(FilterChain.class);
        var response = new MockHttpServletResponse();
        new MfaGateFilter().doFilter(publicReq, response, chain);
        verify(chain).doFilter(publicReq, response);
        var privateReq = new MockHttpServletRequest("POST", "/staff/mobile/pair");
        privateReq.setServletPath("/staff/mobile/pair");
        var blocked = new MockHttpServletResponse();
        new MfaGateFilter().doFilter(privateReq, blocked, mock(FilterChain.class));
        assertThat(blocked.getRedirectedUrl()).isEqualTo("/mfa/continue");
    }

    @Test void verifiedSessionCanEnterAndReauthenticationRequiresNewVerification() throws Exception {
        User user = new User(); user.setUserId(77);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/staff/index");
        req.setServletPath("/staff/index");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MfaGateFilter.VERIFIED_USER_ID, 77);
        req.setSession(session);
        FilterChain chain = mock(FilterChain.class);
        new MfaGateFilter().doFilter(req, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
    }
}
