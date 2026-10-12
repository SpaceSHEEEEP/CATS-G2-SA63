package sg.edu.iss.cats.captcha;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class CaptchaValidationFilter extends OncePerRequestFilter {
    private final CaptchaService service;
    private static final Map<String, String> POST_SCOPES = Map.of(
            "/staff/login", "login-staff",
            "/admin/login", "login-admin",
            "/staff/mobile/pair", "pair-code",
            "/staff/claim", "fee-claim",
            "/admin/users/email", "admin-mail");

    public CaptchaValidationFilter(CaptchaService service) { this.service = service; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
            FilterChain chain) throws ServletException, IOException {
        if (!"POST".equals(req.getMethod())) { chain.doFilter(req, res); return; }
        String scope = POST_SCOPES.get(req.getServletPath());
        if (scope == null) { chain.doFilter(req, res); return; }
        if (service.verify(req.getSession(false), scope,
                req.getParameter("captchaId"), req.getParameter("captchaAnswer"))) {
            chain.doFilter(req, res); return;
        }
        String destination = switch (scope) {
            case "login-admin" -> "/admin/login";
            case "login-staff" -> "/staff/login";
            case "pair-code" -> "/staff/mobile";
            case "fee-claim" -> "/staff/claims";
            case "admin-mail" -> "/admin/index";
            default -> "/staff/login";
        };
        res.sendRedirect(req.getContextPath() + destination + "?captchaError=1");
    }
}
