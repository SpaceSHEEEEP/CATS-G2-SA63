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

@Configuration 
@EnableWebSecurity 
public class SecurityConfig {

    @Bean 
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
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
                    boolean isAdmin = authentication.getAuthorities().stream()
                                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    if (isAdmin) response.sendRedirect(request.getContextPath() + "/admin/index");
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
            .build();
    }

    @Bean 
    @Order(2)
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/staff/login", "/admin/login", "/style.css", "/error").permitAll()
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/staff/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                .requestMatchers("/api/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/staff/login")
                .defaultSuccessUrl("/staff/index")
            )
            .logout(logout -> logout 
                .logoutUrl("/logout")
                .logoutSuccessUrl("/staff/login?logout")
            )
            .build();
    }

    @Bean 
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
