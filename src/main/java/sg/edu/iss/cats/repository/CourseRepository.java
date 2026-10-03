package sg.edu.iss.cats.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseType;

public interface CourseRepository extends JpaRepository<Course, Integer> {
	
	List<Course> findByCourseTypeOrderByStartDateAsc(CourseType courseType);

    Page<Course> findByCourseTypeOrderByStartDateAsc(CourseType courseType, Pageable pageable);

}
