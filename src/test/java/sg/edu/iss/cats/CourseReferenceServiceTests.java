package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.*;

class CourseReferenceServiceTests {
    private final CourseCategoryRepository categories = mock(CourseCategoryRepository.class);
    private final TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final CourseReferenceService service = new CourseReferenceService(categories, providers, courses);

    @Test void resolvesSelectedProviderAndCategoryFromDatabase() {
        CourseCategory type = new CourseCategory(); type.setCode(CourseType.EXTERNAL); type.setEnabled(true);
        TrainingProvider provider = new TrainingProvider(); provider.setId(12);
        provider.setName("Verified Training Provider"); provider.setEnabled(true);
        when(categories.findById(CourseType.EXTERNAL)).thenReturn(Optional.of(type));
        when(providers.findById(12)).thenReturn(Optional.of(provider));
        Course course = new Course(); course.setCourseType(CourseType.EXTERNAL); course.setProviderId(12);
        service.resolveCourse(course);
        assertThat(course.getTrainingProvider()).isEqualTo("Verified Training Provider");
        assertThat(course.getProvider()).isSameAs(provider);
    }

    @Test void disabledCategoryCannotAcceptNewApplications() {
        CourseCategory type = new CourseCategory(); type.setCode(CourseType.INTERNAL); type.setEnabled(false);
        when(categories.findById(CourseType.INTERNAL)).thenReturn(Optional.of(type));
        Course course = new Course(); course.setCourseType(CourseType.INTERNAL); course.setProviderId(12);
        assertThatThrownBy(() -> service.resolveCourse(course))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("inactive");
        verifyNoInteractions(providers);
    }

    @Test void providerWithCourseHistoryCannotBeDeleted() {
        TrainingProvider provider = new TrainingProvider(); provider.setId(12);
        when(providers.findById(12)).thenReturn(Optional.of(provider));
        when(courses.existsByProvider_Id(12)).thenReturn(true);
        assertThatThrownBy(() -> service.deleteProvider(12))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("history");
        verify(providers, never()).delete(any());
    }
}
