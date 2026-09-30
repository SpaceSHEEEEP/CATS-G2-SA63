package sg.edu.iss.cats.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.repository.ApplicationRepository;
import sg.edu.iss.cats.repository.CourseRepository;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CourseRepository courseRepository;

    public ApplicationService(ApplicationRepository applicationRepository, CourseRepository courseRepository) {
        this.applicationRepository = applicationRepository;
        this.courseRepository = courseRepository;
    }
    
    @Transactional( 
    propagation  = Propagation.REQUIRED,
    isolation    = Isolation.SERIALIZABLE,
    rollbackFor  = Exception.class,
    timeout      = 30
    )
    // public Application saveApplication(Application application) {
    public void saveApplication(Application application) {  

        // if (application.getApplicationStatus() == ApplicationStatus.APPLIED){
        //     application.setApplicationStatus(ApplicationStatus.UPDATED);
        // }
        application.setApplicationStatus(ApplicationStatus.APPLIED);

        Course c = application.getCourse();
        if (c != null) { // application shouldn't have null course
            if (c.getCourseId() == null) {
                // this is a new course, save it in course sql table
                Course newCourse = new Course(  
                        c.getCourseName(), 
                        c.getCourseType(), 
                        c.getStartDate(), 
                        c.getEndDate(), 
                        c.getLocation(), 
                        c.getTrainingProvider(), 
                        c.getFee(), 
                        c.getDuration());
                courseRepository.save(newCourse);
                application.setCourse(newCourse);
            }
            else {
                // delete these. it works
                System.out.println("DEBUG - Course ID: " + (c != null ? c.getCourseId() : "Course is null"));
                System.out.println("DEBUG - Course Name: " + (c != null ? c.getCourseName() : "N/A"));
                System.out.println("DEBUG - Course Type: " + (c != null ? c.getCourseType() : "N/A"));
            }

        // TODO: complete validation

        // course title, period, category, justification filled up 
        // if (c.)




        // start date before end date 
        
        // start date must be in the future

        // weekends and public holidays falling during training period are not counted

        // half day sessions are only for internal training 

        // start and end dates must be working days

        // user must have enough budget 

        // TODO: The course period must not overlap with another ‘Applied’, ‘Updated’ 
        // or ‘Approved’ course application of the same employee. (to be done after added manager features)

        }
        applicationRepository.save(application);
       
    }


}
