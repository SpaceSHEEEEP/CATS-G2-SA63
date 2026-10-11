package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Designation;
import sg.edu.iss.cats.repository.UserRepository;

/** Explicit one-time initial admin provisioning, never implicit or demo credentials. */
@Configuration
@ConditionalOnProperty(name = "cats.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminService {
    @Bean
    ApplicationRunner provisionInitialAdministrator(UserRepository users, PasswordEncoder encoder, PlatformTransactionManager tm,
            @Value("${cats.bootstrap-admin.username}") String username,
            @Value("${cats.bootstrap-admin.password}") String password,
            @Value("${cats.bootstrap-admin.email}") String email,
            @Value("${cats.bootstrap-admin.name}") String name) {
        return args -> new TransactionTemplate(tm).executeWithoutResult(status ->
                provision(users, encoder, username, password, email, name));
    }

    public void provision(UserRepository users, PasswordEncoder encoder, String username,
            String password, String email, String name) {
        if (users.existsByRole(Role.ROLE_ADMIN)) return;
        if (username.isBlank() || password.length() < 12 || !email.contains("@") || name.isBlank())
            throw new IllegalStateException("BOOTSTRAP_ADMIN_* settings must provide name, email, username and a >=12-character password");
        if (users.existsByUsername(username))
            throw new IllegalStateException("Cannot bootstrap administrator: username already in use");
        User admin = new User();
        admin.setName(name.strip()); admin.setUsername(username.strip());
        admin.setEmail(email.strip()); admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ROLE_ADMIN); admin.setAdmin(true);
        admin.setActive(true); admin.setDesignation(Designation.PROFESSIONAL);
        admin.setActualDays(0.0); admin.setBudgetedDays(0.0);
        admin.setActualAllowance(BigDecimal.ZERO); admin.setBudgetedAllowance(BigDecimal.ZERO);
        users.saveAndFlush(admin);
    }
}
