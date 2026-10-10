package sg.edu.iss.cats.service;

import java.time.Year;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.dto.AdminUserForm;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;

@Service
public class AdminStaffService {
    private final UserRepository users;
    private final TrainingAccountService accounts;
    private final PasswordEncoder encoder;

    public AdminStaffService(UserRepository users, TrainingAccountService accounts, PasswordEncoder encoder) {
        this.users = users; this.accounts = accounts; this.encoder = encoder;
    }

    public List<User> eligibleManagers() {
        return users.findAll().stream()
                .filter(u -> u.isEnabled() && u.getRole() == Role.ROLE_MANAGER)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())).toList();
    }

    @Transactional
    public AdminUserForm formFor(Integer id, int year) {
        User user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        TrainingAccountService.Summary yearSummary = accounts.summary(user, year);
        AdminUserForm form = new AdminUserForm();
        form.setUserId(user.getUserId()); form.setName(user.getName());
        form.setUsername(user.getUsername()); form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        form.setDesignation(user.getDesignation() == null ? Designation.PROFESSIONAL : user.getDesignation());
        form.setManagerId(user.getManager() == null ? null : user.getManager().getUserId());
        form.setYear(year); form.setAnnualDays(yearSummary.getDayLimit());
        form.setAnnualAllowance(yearSummary.getFeeLimit());
        form.setEnabled(user.isEnabled());
        return form;
    }

    @Transactional
    public User save(AdminUserForm form, Integer adminId) {
        if (form.getName() == null || form.getName().isBlank()
                || form.getUsername() == null || form.getUsername().isBlank()
                || form.getEmail() == null || form.getEmail().isBlank()
                || form.getRole() == null || form.getDesignation() == null
                || form.getAnnualDays() == null || form.getAnnualDays() < 0
                || form.getAnnualAllowance() == null || form.getAnnualAllowance().signum() < 0) {
            throw new IllegalArgumentException("Missing or invalid employee details");
        }
        boolean creating = form.getUserId() == null;
        User user = creating ? new User() : users.findById(form.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (users.existsByUsernameAndUserIdNot(form.getUsername(),
                creating ? -1 : user.getUserId()))
            throw new IllegalArgumentException("Username is already taken");
        if (creating && (form.getPassword() == null || form.getPassword().isBlank()))
            throw new IllegalArgumentException("A password is required for new employees");
        if (user.getUserId() != null && user.getUserId().equals(adminId)
                && (!form.isEnabled() || form.getRole() != Role.ROLE_ADMIN))
            throw new IllegalArgumentException("Cannot remove your own admin access");
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (form.getPassword().length() < 10)
                throw new IllegalArgumentException("Password must contain at least 10 characters");
            user.setPassword(encoder.encode(form.getPassword()));
        }
        user.setName(form.getName().trim());
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setRole(form.getRole());
        user.setAdmin(form.getRole() == Role.ROLE_ADMIN); // legacy flag, kept consistent
        user.setDesignation(form.getDesignation());
        user.setActive(form.isEnabled());
        if (form.getManagerId() == null) {
            user.setManager(null);
        } else {
            User supervisor = users.findById(form.getManagerId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected manager not found"));
            if (!supervisor.isEnabled() || supervisor.getRole() != Role.ROLE_MANAGER)
                throw new IllegalArgumentException("Supervisor must be an active manager");
            for (User ancestor = supervisor; ancestor != null; ancestor = ancestor.getManager()) {
                if (ancestor == user || (user.getUserId() != null && user.getUserId().equals(ancestor.getUserId())))
                    throw new IllegalArgumentException("Management hierarchy cannot contain cycles");
            }
            user.setManager(supervisor);
        }
        if (!creating && (!form.isEnabled() || form.getRole() != Role.ROLE_MANAGER)
                && users.existsByManager_UserId(user.getUserId()))
            throw new IllegalArgumentException("Reassign this user's subordinates first");
        if (creating) {
            user.setActualDays(form.getAnnualDays());
            user.setBudgetedDays(form.getAnnualDays());
            user.setActualAllowance(form.getAnnualAllowance());
            user.setBudgetedAllowance(form.getAnnualAllowance());
        }
        user = users.saveAndFlush(user);
        accounts.updateAllocation(user, form.getYear(), form.getAnnualDays(), form.getAnnualAllowance());
        if (form.getYear() == Year.now(java.time.ZoneId.of("Asia/Singapore")).getValue())
            accounts.syncLegacyCurrentYear(user);
        return user;
    }

    /** Reversible deactivation preserves applications, approvals and audit history. */
    @Transactional
    public void deactivate(Integer userId, Integer adminId) {
        if (userId.equals(adminId)) throw new IllegalArgumentException("Cannot deactivate your own account");
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (users.existsByManager_UserId(userId))
            throw new IllegalArgumentException("Reassign subordinates before deactivation");
        user.setActive(false);
        users.save(user);
    }
}
