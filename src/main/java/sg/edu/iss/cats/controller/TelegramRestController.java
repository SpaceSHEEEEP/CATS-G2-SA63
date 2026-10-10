package sg.edu.iss.cats.controller;

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.dto.CourseDTO;

//using RestController allows Spring to serializes the returned objects into JSON
@RestController
@RequestMapping("/api")
public class TelegramRestController {

	@Autowired
	private CourseRepository courseRepository;

	@GetMapping("/courses")
	public List<CourseDTO> getCourses() {

	    List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getArchived())).toList();
	    List<CourseDTO> courseDTOs = new ArrayList<>();

	    for (Course course : courses) {

	        CourseDTO dto = new CourseDTO(
	        		course.getCourseId(),
	                course.getCourseName(),
	                course.getCourseType(),
	                course.getStartDate(),
	                course.getEndDate(),
	                course.getLocation(),
	                course.getTrainingProvider(),
	                course.getFee()
	        		);

	        courseDTOs.add(dto);
	    }

	    return courseDTOs;
	}
	
	@GetMapping("/courses/{courseId}")
	public CourseDTO getCourse(
	        @PathVariable(name = "courseId") Integer courseId) {

        Course course = courseRepository.findById(courseId)
                .filter(c -> !Boolean.TRUE.equals(c.getArchived()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Course not found"));
	    return new CourseDTO(
        		course.getCourseId(),
                course.getCourseName(),
                course.getCourseType(),
                course.getStartDate(),
                course.getEndDate(),
                course.getLocation(),
                course.getTrainingProvider(),
                course.getFee()
        		);
	}	
}