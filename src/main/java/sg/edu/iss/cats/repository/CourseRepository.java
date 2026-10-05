package sg.edu.iss.cats.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseType;

public interface CourseRepository extends JpaRepository<Course, Integer> {
	
	List<Course> findByCourseTypeOrderByStartDateAsc(CourseType courseType);
	
	// Query result containing a course and its calculated applicant count.
	// This is not an entity and does not create another database table.
	interface CourseApplicantSummary {
	    Course getCourse();
	    Long getApplicantCount();
	}

	// Provisional rule: count distinct employees across all application statuses.
	@Query("""
	        SELECT c AS course,
	               COUNT(DISTINCT a.user.userId) AS applicantCount
	        FROM Course c
	        LEFT JOIN c.applications a
	        GROUP BY c
	        ORDER BY COUNT(DISTINCT a.user.userId) DESC, c.courseId ASC
	        """)
	List<CourseApplicantSummary> findCoursesWithApplicantCounts();

    Page<Course> findByCourseTypeOrderByStartDateAsc(CourseType courseType, Pageable pageable);

    @EntityGraph(attributePaths = {"course, application"})
    Page<Course> findAllByOrderByStartDateAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"applications"})
    @Query("SELECT c FROM Course c ORDER BY SIZE(c.applications) DESC")
    Page<Course> findAllByOrderByApplicationsSizeDesc(Pageable pageable);

}
