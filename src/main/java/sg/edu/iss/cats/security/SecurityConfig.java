package sg.edu.iss.cats.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import sg.edu.iss.cats.mfa.MfaGateFilter;
import sg.edu.iss.cats.captcha.CaptchaService;
import sg.edu.iss.cats.captcha.CaptchaValidationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain mobileTokenFilterChain(HttpSecurity http,
            CorsConfigurationSource mobileCorsConfigurationSource) throws Exception {
        return http.securityMatcher("/api/mobile/**")
                .cors(cors -> cors.configurationSource(mobileCorsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http, CaptchaService captcha) throws Exception {
        return http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth
                // All users can see these
                .requestMatchers("/admin/login", "/style.css", "/error").permitAll()

                //Only ADMIN can go here
                .requestMatchers(HttpMethod.GET, "/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/admin/**").hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .successHandler((request, response, authentication) -> {
                    // Clear any previous MFA verification even when Spring migrates session attributes.
                    request.getSession().removeAttribute(MfaGateFilter.VERIFIED_USER_ID);
                    boolean isAdmin = authentication.getAuthorities().stream()
                                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    if (isAdmin) response.sendRedirect(request.getContextPath() + "/mfa/continue?target=admin");
                    else {
                        new SecurityContextLogoutHandler().logout(request, response, authentication);
                        response.sendRedirect(request.getContextPath() + "/staff/login?adminDenied");
                    }
                })
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
            )
            .addFilterBefore(new CaptchaValidationFilter(captcha), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new MfaGateFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain userFilterChain(HttpSecurity http, CaptchaService captcha) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/staff/login", "/admin/login", "/style.css", "/error",
                        "/images/**", "/js/**", "/captcha/**", "/mobile/app/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/fonts/**").permitAll()
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/staff/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                .requestMatchers("/api/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/staff/login")
                .successHandler((request, response, authentication) -> {
                    request.getSession().removeAttribute(MfaGateFilter.VERIFIED_USER_ID);
                    response.sendRedirect(request.getContextPath() + "/mfa/continue?target=staff");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/staff/login?logout")
            )
            .addFilterBefore(new CaptchaValidationFilter(captcha), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new MfaGateFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    CorsConfigurationSource mobileCorsConfigurationSource(
            @Value("${cats.mobile.android-origins:https://localhost,capacitor://localhost}") String origins) {
        CorsConfiguration mobile = new CorsConfiguration();
        mobile.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim)
                .filter(x -> !x.isBlank()).toList());
        mobile.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        mobile.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        mobile.setAllowCredentials(false); // bearer tokens, never session cookies
        mobile.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/mobile/**", mobile);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
