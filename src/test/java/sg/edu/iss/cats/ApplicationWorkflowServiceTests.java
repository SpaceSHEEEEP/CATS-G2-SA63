package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.*;

class ApplicationWorkflowServiceTests {
    private final AppRepo apps = mock(AppRepo.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final TrainingAccountService accounts = mock(TrainingAccountService.class);
    private final ApplicationWorkflowService workflow =
        new ApplicationWorkflowService(apps, courses, users, comments, accounts);

    private User user(int id, Role role) {
        User u = new User(); u.setUserId(id); u.setRole(role); return u;
    }

    @Test void managerCannotApproveOtherTeamsApplication() {
        User manager = user(5, Role.ROLE_MANAGER);
        User owner = user(7, Role.ROLE_EMPLOYEE);
        owner.setManager(user(6, Role.ROLE_MANAGER));
        Application a = new Application(); a.setId(12); a.setUser(owner); a.setStatus(Status.APPLIED);
        when(users.findLockedById(5)).thenReturn(java.util.Optional.of(manager));
        when(apps.findById(12)).thenReturn(java.util.Optional.of(a));
        assertThatThrownBy(() -> workflow.decide(5, 12, Status.APPROVED, "Appropriate for role"))
            .isInstanceOf(SecurityException.class);
        verify(apps, never()).saveAndFlush(any());
    }

    @Test void managerCannotDecideACompletedRecord() {
        User manager = user(5, Role.ROLE_MANAGER);
        User owner = user(7, Role.ROLE_EMPLOYEE); owner.setManager(manager);
        Application a = new Application(); a.setId(12); a.setUser(owner); a.setStatus(Status.COMPLETED);
        when(users.findLockedById(5)).thenReturn(java.util.Optional.of(manager));
        when(apps.findById(12)).thenReturn(java.util.Optional.of(a));
        assertThatThrownBy(() -> workflow.decide(5, 12, Status.REJECTED, "Not relevant"))
            .isInstanceOf(IllegalArgumentException.class);
        verify(apps, never()).saveAndFlush(any());
    }

    @Test void cannotCompleteAnApprovedFutureCourse() {
        User employee = user(8, Role.ROLE_EMPLOYEE);
        Course c = new Course(); c.setEndDate(LocalDate.now().plusDays(20));
        Application a = new Application(); a.setId(9); a.setUser(employee);
        a.setCourse(c); a.setStatus(Status.APPROVED);
        when(users.findLockedById(8)).thenReturn(java.util.Optional.of(employee));
        when(apps.findById(9)).thenReturn(java.util.Optional.of(a));
        assertThatThrownBy(() -> workflow.completeApplication(9, 8, "Useful training"))
            .isInstanceOf(IllegalArgumentException.class);
        verify(comments, never()).save(any());
    }

    @Test void cannotDeleteAnotherEmployeesApplication() {
        User employee = user(8, Role.ROLE_EMPLOYEE);
        Application a = new Application(); a.setId(9); a.setUser(user(11, Role.ROLE_EMPLOYEE));
        a.setStatus(Status.APPLIED);
        when(users.findLockedById(8)).thenReturn(java.util.Optional.of(employee));
        when(apps.findById(9)).thenReturn(java.util.Optional.of(a));
        assertThatThrownBy(() -> workflow.deleteApplication(a, 8))
            .isInstanceOf(SecurityException.class);
        verify(apps, never()).saveAndFlush(any());
    }
}
