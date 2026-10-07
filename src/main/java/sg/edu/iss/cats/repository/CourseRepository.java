package sg.edu.iss.cats.repository;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.dto.CourseApplicantSummary;

public interface CourseRepository extends JpaRepository<Course, Integer> {
	
	// Query result containing a course and its calculated applicant count.
	// This is not an entity and does not create another database table.
	// Provisional rule: count distinct employees across all application statuses.
	@Query("""
	        SELECT c AS course,
	               COUNT(DISTINCT a.user.userId) AS applicantCount
	        FROM Course c
	        LEFT JOIN c.applications a
	        GROUP BY c
	        ORDER BY COUNT(DISTINCT a.user.userId) DESC, c.courseId ASC
	        """)
	Page<CourseApplicantSummary> findCoursesWithApplicantCounts(Pageable pageable);
	// List<CourseApplicantSummary> findCoursesWithApplicantCounts();

    Page<Course> findByCourseTypeInOrderByStartDateAsc(Collection<CourseType> types, Pageable pageable);

    @EntityGraph(attributePaths = {"course, application"})
    Page<Course> findAllByOrderByStartDateAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"applications"})
    @Query("SELECT c FROM Course c ORDER BY SIZE(c.applications) DESC")
    Page<Course> findAllByOrderByApplicationsSizeDesc(Pageable pageable);
}
