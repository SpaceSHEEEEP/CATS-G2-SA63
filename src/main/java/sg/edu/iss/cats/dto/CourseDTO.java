package sg.edu.iss.cats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import sg.edu.iss.cats.model.CourseType;

@Data
//@data includes @getter and @setter from lombok
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private Integer courseId;
    private String courseName;
    private CourseType courseType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private String trainingProvider;
    private BigDecimal fee;

    
//    public CourseDTO(
//            Integer courseId,
//            String courseName,
//            CourseType courseType,
//            LocalDate startDate,
//            LocalDate endDate,
//            String location,
//            String trainingProvider,
//            BigDecimal fee) {
//
//        this.courseId = courseId;
//        this.courseName = courseName;
//        this.courseType = courseType;
//        this.startDate = startDate;
//        this.endDate = endDate;
//        this.location = location;
//        this.trainingProvider = trainingProvider;
//        this.fee = fee;
//    }

    //empty constructor required for JSON to respond back to
//    public CourseDTO() {
//    }
    
    
//    public Integer getCourseId() {
//        return courseId;
//    }
//    public String getCourseName() {
//        return courseName;
//    }
//    public CourseType getCourseType() {
//        return courseType;
//    }
//    public LocalDate getStartDate() {
//        return startDate;
//    }
//    public LocalDate getEndDate() {
//        return endDate;
//    }
//    public String getLocation() {
//        return location;
//    }
//    public String getTrainingProvider() {
//        return trainingProvider;
//    }
//    public BigDecimal getFee() {
//        return fee;
//    }
//    
//    public void setCourseId(int id) {
//    	this.courseId = id;
//    }


}