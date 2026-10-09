package sg.edu.iss.cats.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration 
@EnableWebSecurity 
public class SecurityConfig {

    @Bean 
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                    // All users can see these
                    .requestMatchers("/staff/login", "/style.css").permitAll()

                    // Only EMPLOYEEs can see urls that start with /staff
                    .requestMatchers(HttpMethod.GET, "/staff/**").hasAnyAuthority("EMPLOYEE", "MANAGER")
                    .requestMatchers(HttpMethod.POST, "/staff/**").hasAnyAuthority("EMPLOYEE", "MANAGER")

                    // any other urls, user just needs to be authenticated
                    .anyRequest().authenticated()
                    )
            .formLogin(form -> form
                    .loginPage("/staff/login")
                    .loginProcessingUrl("/staff/login")
                    .defaultSuccessUrl("/staff/index")
            )
            .logout(Customizer.withDefaults())
            .build();
    }

    @Bean 
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
@Bean
public CommandLineRunner testEncoder(PasswordEncoder encoder) {
    return args -> {
        System.out.println("HASHED PW IS: " + encoder.encode("pw"));
    };
}
}
