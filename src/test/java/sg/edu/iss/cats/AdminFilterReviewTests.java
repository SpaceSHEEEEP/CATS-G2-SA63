package sg.edu.iss.cats;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ExtendedModelMap;
import sg.edu.iss.cats.controller.AdminController;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.HolidayRepository;
import sg.edu.iss.cats.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminFilterReviewTests {
    private User admin() {
        User user = new User();
        user.setUserId(7);
        user.setRole(Role.ROLE_ADMIN);
        return user;
    }

    @Test void typedCatalogueFilterRetainsOverallCount() {
        var users = mock(UserRepository.class);
        var holidays = mock(HolidayRepository.class);
        var courses = mock(CourseRepository.class);
        var admin = admin();
        when(users.findById(7)).thenReturn(Optional.of(admin));
        when(users.findAllByOrderByUserIdAsc()).thenReturn(List.of(admin));
        when(holidays.findByHolidayDateBetweenOrderByHolidayDateAsc(any(), any())).thenReturn(List.of());
        when(courses.findCoursesWithApplicantCountsByType(eq(CourseType.EXTERNAL), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(courses.count()).thenReturn(12L);
        var model = new ExtendedModelMap();
        assertEquals("admin", new AdminController(users, holidays, courses)
                .showAdmin(admin, 0, "EXTERNAL", model));
        assertEquals("EXTERNAL", model.get("selectedType"));
        assertEquals(12L, model.get("catalogueCourseCount"));
        verify(courses).findCoursesWithApplicantCountsByType(eq(CourseType.EXTERNAL), any(Pageable.class));
        verify(courses, never()).findCoursesWithApplicantCounts(any(Pageable.class));
    }

    @Test void invalidCategoryFallsBackToUnfilteredCatalogue() {
        var users = mock(UserRepository.class);
        var holidays = mock(HolidayRepository.class);
        var courses = mock(CourseRepository.class);
        var admin = admin();
        when(users.findById(7)).thenReturn(Optional.of(admin));
        when(users.findAllByOrderByUserIdAsc()).thenReturn(List.of(admin));
        when(holidays.findByHolidayDateBetweenOrderByHolidayDateAsc(any(), any())).thenReturn(List.of());
        when(courses.findCoursesWithApplicantCounts(any(Pageable.class))).thenReturn(Page.empty());
        var model = new ExtendedModelMap();
        assertEquals("admin", new AdminController(users, holidays, courses)
                .showAdmin(admin, -100, "unexpected", model));
        assertEquals("ALL", model.get("selectedType"));
        assertEquals(0, model.get("pageNum"));
        verify(courses, never()).findCoursesWithApplicantCountsByType(any(), any());
    }
}
