package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.DataImportService;

class DataImportServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final HolidayRepository holidays = mock(HolidayRepository.class);
    private final TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
    private final AppRepo applications = mock(AppRepo.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final DataImportService service = new DataImportService(
            users, courses, holidays, providers, applications, json);

    private byte[] originalData() throws Exception {
        return Files.readAllBytes(Path.of("import-data", "demo-import.json"));
    }

    @Test void malformedAndOversizedUploadsCannotWrite() {
        assertThatThrownBy(() -> service.importFile("not JSON".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.importFile(new byte[DataImportService.MAX_BYTES + 1]))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(users, courses, holidays, providers, applications);
    }

    @Test void rejectsExistingUserBeforeSavingAnything() throws Exception {
        when(users.existsByUsername("bobby")).thenReturn(true);
        assertThatThrownBy(() -> service.importFile(originalData()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("bobby");
        verify(users, never()).save(any());
        verify(courses, never()).save(any());
        verify(applications, never()).save(any());
    }

    @Test void importPreservesOriginalAccountsRolesAndApplications() throws Exception {
        when(courses.findAll()).thenReturn(List.of());
        when(providers.findAll()).thenReturn(List.of());
        when(providers.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(courses.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AtomicInteger nextId = new AtomicInteger();
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            if (user.getUserId() == null) user.setUserId(nextId.incrementAndGet());
            return user;
        });
        var expected = new DataImportService.Summary(10, 11, 26, 15, 14);
        assertThat(service.importFile(originalData())).isEqualTo(expected);
        verify(applications, times(14)).save(any(Application.class));
        var usersCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users, atLeast(10)).save(usersCaptor.capture());
        assertThat(usersCaptor.getAllValues()).allSatisfy(user -> {
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.getPassword()).isEqualTo(
                    "$2a$10$XX.htgpTqjfQG5DH0kCA5OtqbYh7t1pxWI3hkXChs8Qu9/LCUGDjy");
            assertThat(user.getEmail()).endsWith("@gmail.com");
        });
        assertThat(usersCaptor.getAllValues()).anySatisfy(user -> {
            assertThat(user.getUsername()).isEqualTo("minny");
            assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
            assertThat(user.isAdmin()).isTrue();
        });
        var applicationsCaptor = org.mockito.ArgumentCaptor.forClass(Application.class);
        verify(applications, times(14)).save(applicationsCaptor.capture());
        assertThat(applicationsCaptor.getAllValues()).allSatisfy(app -> {
            assertThat(app.getUser()).isNotNull();
            assertThat(app.getCourse()).isNotNull();
        });
    }

    @Test void acceptsProviderOnlyImportWithoutUsersOrCourses() {
        byte[] data = """
                {"format":"cats-import-v1", "providers":[{"name":"New Provider","enabled":true}]}
                """.getBytes(StandardCharsets.UTF_8);
        when(providers.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.importFile(data)).isEqualTo(new DataImportService.Summary(0,1,0,0,0));
        verify(providers).save(any());
        verify(users, never()).save(any());
    }
}
