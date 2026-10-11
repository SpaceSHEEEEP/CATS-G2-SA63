package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.service.ReportCsvService;
import sg.edu.iss.cats.service.TrainingReportService;

class ReportCsvServiceTests {
    @Test void attendanceExportUsesApprovedQueryAndNeutralisesSpreadsheetFormulas() {
        TrainingReportService reports = mock(TrainingReportService.class);
        LocalDate from = LocalDate.of(2027, 3, 1), to = LocalDate.of(2027, 3, 31);
        User user = new User(); user.setUserId(3); user.setName("=DANGEROUS()");
        Course course = new Course(); course.setCourseName("Spring");
        course.setCourseType(CourseType.EXTERNAL); course.setFee(new BigDecimal("120.00"));
        course.setStartDate(from); course.setEndDate(to);
        Application app = new Application(); app.setUser(user); app.setCourse(course); app.setStatus(Status.APPROVED);
        when(reports.attendancePage(from, to, CourseType.EXTERNAL, null, PageRequest.of(0, 200)))
                .thenReturn(new PageImpl<>(List.of(app)));
        String csv = new String(new ReportCsvService(reports).attendance(from, to, CourseType.EXTERNAL, null),
                StandardCharsets.UTF_8);
        assertThat(csv).contains("\"'=DANGEROUS()\"").contains("\"APPROVED\"");
        verify(reports).attendancePage(from, to, CourseType.EXTERNAL, null, PageRequest.of(0, 200));
    }
}
