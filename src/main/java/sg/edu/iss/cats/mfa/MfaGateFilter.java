package sg.edu.iss.cats.mfa;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import sg.edu.iss.cats.model.User;

public final class MfaGateFilter extends OncePerRequestFilter {
    public static final String VERIFIED_USER_ID = "cats.mfaVerifiedUserId";
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        // Login, MFA enrollment, health, read-only pairing redemption, and public assets.
        // A mobile snapshot is independently authenticated with a hashed device token.
        if (path.startsWith("/mfa/") || path.startsWith("/captcha/") || path.startsWith("/api/mobile/")
                || path.equals("/staff/login") || path.equals("/admin/login")
                || path.equals("/logout") || path.equals("/admin/logout")
                || path.equals("/style.css") || path.startsWith("/images/")
                || path.startsWith("/js/") || path.startsWith("/mobile/app/")
                || path.equals("/actuator/health") || path.equals("/error")) {
            chain.doFilter(request, response); return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            HttpSession session = request.getSession(false);
            if (session == null || !user.getUserId().equals(session.getAttribute(VERIFIED_USER_ID))) {
                if (path.startsWith("/api/") || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    response.sendError(401, "MFA required");
                } else {
                    response.sendRedirect(request.getContextPath() + "/mfa/continue");
                }
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
