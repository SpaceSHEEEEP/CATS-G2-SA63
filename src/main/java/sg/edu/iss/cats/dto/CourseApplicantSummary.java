package sg.edu.iss.cats.dto;

import sg.edu.iss.cats.model.Course;

public interface CourseApplicantSummary {
    Course getCourse();
    Long getApplicantCount();
}
