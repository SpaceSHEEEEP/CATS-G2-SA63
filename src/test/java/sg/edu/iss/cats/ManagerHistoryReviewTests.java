package sg.edu.iss.cats;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ExtendedModelMap;
import sg.edu.iss.cats.controller.ManagerController;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.service.ApplicationWorkflowService;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManagerHistoryReviewTests {
    private final UserRepository users = mock(UserRepository.class);
    private final AppRepo applications = mock(AppRepo.class);
    private final ApplicationWorkflowService workflows = mock(ApplicationWorkflowService.class);
    private final ManagerController controller = new ManagerController(users, applications, workflows);

    private User manager() {
        var user = new User(); user.setRole(Role.ROLE_MANAGER); user.setUserId(15);
        return user;
    }

    @Test void fullYearHistoryRemainsAvailableWhenChosen() {
        when(applications.findCurrentYearTeam(eq(15), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        var model = new ExtendedModelMap();
        assertEquals("history", controller.showSubordinateHistory(manager(), 0, "ALL", model));
        var paging = ArgumentCaptor.forClass(Pageable.class);
        verify(applications).findCurrentYearTeam(eq(15), any(LocalDate.class), any(LocalDate.class), paging.capture());
        assertTrue(paging.getValue().isUnpaged());
        assertEquals("ALL", model.get("historyPageSize"));
    }

    @Test void pagedViewIsStillAvailableAndBounded() {
        when(applications.findCurrentYearTeam(eq(15), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(2, 20)));
        var model = new ExtendedModelMap();
        assertEquals("history", controller.showSubordinateHistory(manager(), 2, "20", model));
        var paging = ArgumentCaptor.forClass(Pageable.class);
        verify(applications).findCurrentYearTeam(eq(15), any(LocalDate.class), any(LocalDate.class), paging.capture());
        assertEquals(2, paging.getValue().getPageNumber());
        assertEquals(20, paging.getValue().getPageSize());
        assertEquals("20", model.get("historyPageSize"));
    }

    @Test void nonManagersCannotInspectSubordinateHistory() {
        var employee = new User(); employee.setRole(Role.ROLE_EMPLOYEE);
        assertEquals("redirect:/staff/index",
                controller.showSubordinateHistory(employee, 0, "ALL", new ExtendedModelMap()));
        verifyNoInteractions(applications);
    }
}
