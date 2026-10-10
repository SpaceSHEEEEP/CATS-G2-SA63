package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;

@Service
public class CourseCatalogueService {
    private final CourseRepository courses;
    private final AppRepo applications;
    public CourseCatalogueService(CourseRepository courses, AppRepo applications) {
        this.courses = courses; this.applications = applications;
    }

    @Transactional
    public Course save(Course form) {
        if (form.getCourseName() == null || form.getCourseName().isBlank()
                || form.getCourseType() == null || form.getDuration() == null
                || form.getStartDate() == null || form.getEndDate() == null
                || form.getEndDate().isBefore(form.getStartDate())
                || form.getTrainingProvider() == null || form.getTrainingProvider().isBlank()
                || form.getLocation() == null || form.getLocation().isBlank()
                || form.getFee() == null || form.getFee().signum() < 0
                || (form.getCourseType() == CourseType.INTERNAL && form.getFee().compareTo(BigDecimal.ZERO) != 0)
                || (form.getCourseType() != CourseType.INTERNAL && form.getDuration() != CourseDuration.FULLDAY)
                || (form.getDuration() != CourseDuration.FULLDAY && !form.getStartDate().equals(form.getEndDate())))
            throw new IllegalArgumentException("Invalid catalogue course details");
        if (form.getCourseId() != null) {
            Course old = courses.findById(form.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));
            if (applications.existsByCourse_CourseId(old.getCourseId())) {
                // Preserve submitted historical snapshots when the catalogue changes.
                old.setArchived(true);
                courses.saveAndFlush(old);
                form.setCourseId(null);
            }
        }
        form.setArchived(false);
        return courses.save(form);
    }

    @Transactional
    public void archive(Integer id) {
        Course course = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        course.setArchived(true);
        courses.save(course);
    }
}
