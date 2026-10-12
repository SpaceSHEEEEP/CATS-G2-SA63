package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.NotificationOutboxRepository;
import sg.edu.iss.cats.service.NotificationOutboxService;

class NotificationOutboxServiceTests {
    @Test void submissionIsQueuedToManagerAndDecisionToEmployee() {
        NotificationOutboxRepository repo = mock(NotificationOutboxRepository.class);
        NotificationOutboxService service = new NotificationOutboxService(repo);
        User manager = new User(); manager.setEmail("manager@example.invalid"); manager.setActive(true);
        User employee = new User(); employee.setName("Staff"); employee.setEmail("staff@example.invalid");
        employee.setManager(manager);
        Course course = new Course(); course.setCourseName("Development");
        Application a = new Application(); a.setId(4); a.setStatus(Status.APPLIED);
        a.setUser(employee); a.setCourse(course);
        service.applicationSubmitted(a);
        a.setStatus(Status.APPROVED); a.setManagerReason("Relevant skills");
        service.applicationDecided(a);
        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getRecipient()).isEqualTo("manager@example.invalid");
        assertThat(captor.getAllValues().get(1).getRecipient()).isEqualTo("staff@example.invalid");
        assertThat(captor.getAllValues().get(1).getBody()).contains("Relevant skills");
    }
}
