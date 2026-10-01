package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseDuration;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.ApplicationRepository;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.HolidayRepository;
import sg.edu.iss.cats.repository.UserRepository;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CourseRepository courseRepository;
    private final HolidayRepository holidayRepository;
    private final UserRepository userRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository, 
            CourseRepository courseRepository,
            HolidayRepository holidayRepository,
            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.courseRepository = courseRepository;
        this.holidayRepository = holidayRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional( 
    propagation  = Propagation.REQUIRED,
    isolation    = Isolation.SERIALIZABLE,
    rollbackFor  = Exception.class,
    timeout      = 30
    )
    public void saveApplication(Application application, Integer userId) {  

        // if (application.getApplicationStatus() == ApplicationStatus.APPLIED){
        //     application.setApplicationStatus(ApplicationStatus.UPDATED);
        // }
        application.setApplicationStatus(ApplicationStatus.APPLIED);

        // ensure that the course is either in the course sql table or it will be added into the sql table
        Course c = application.getCourse();
        if (c != null) { // application shouldn't have null course
            if (c.getCourseId() == null || !courseRepository.existsById(c.getCourseId())) {
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
                courseRepository.saveAndFlush(newCourse); // hibernate doesn't always save immediately, need immediate for next step
                application.setCourse(newCourse);
            }
            else {
                // delete these. it works
                System.out.println("DEBUG - Course ID: " + (c != null ? c.getCourseId() : "Course is null"));
                System.out.println("DEBUG - Course Name: " + (c != null ? c.getCourseName() : "N/A"));
                System.out.println("DEBUG - Course Type: " + (c != null ? c.getCourseType() : "N/A"));
            }

            // VALIDATION
            if (c.getStartDate().isAfter(c.getEndDate())) 
                throw new RuntimeException("Start date must be before end date");
            
            // start date must be in the future
            if (!c.getStartDate().isAfter(LocalDate.now())) 
                throw new RuntimeException("Start date must be in the future");

            // half day sessions are only for internal training 
            if (c.getCourseType() != CourseType.INTERNAL && c.getDuration() != CourseDuration.FULLDAY) 
                throw new RuntimeException("Only internal courses can be half day");

            // start and end dates must be working days             
            if (checkWorkingDay(c.getStartDate()))
                throw new RuntimeException("Start day must be a working day");
            if (checkWorkingDay(c.getEndDate()))
                throw new RuntimeException("End day must be a working day");

            // subtract user.days 
            // weekends and public holidays falling during training period are not counted
            double numOfDays = ChronoUnit.DAYS.between(c.getStartDate(), c.getEndDate()) + 1.0;
            if (c.getDuration() != CourseDuration.FULLDAY) numOfDays = 0.5;
            else {
                LocalDate start = c.getStartDate();
                LocalDate end = c.getEndDate();
                while (!start.isEqual(end)) {
                    if (!checkWorkingDay(start)) numOfDays--;
                    start = start.plusDays(1);
                }
            }

            // user must have enough budget 
            User user = userRepository.findById(userId).orElse(null);
            if (c.getFee().compareTo(user.getBudget()) > 0) 
                throw new RuntimeException("You do not have enough budget to apply to this course");

            // The course period must not overlap with another ‘Applied’, ‘Updated’ 
            // or ‘Approved’ course application of the same employee. (to be done after added manager features)
            List<Application> applications = 
                applicationRepository.findByUser_UserIdAndApplicationStatus(userId, ApplicationStatus.APPLIED);
            applications.addAll(applicationRepository.findByUser_UserIdAndApplicationStatus(userId, ApplicationStatus.UPDATED));
            applications.addAll(applicationRepository.findByUser_UserIdAndApplicationStatus(userId, ApplicationStatus.APPROVED));
            for (Application a : applications) {
                if (c.getStartDate().compareTo(a.getCourse().getEndDate()) <= 0 &&
                    c.getEndDate().compareTo(a.getCourse().getStartDate())  >= 0)
                    throw new RuntimeException("This course overlaps with your " + a.getCourse().getCourseName() + " course. Please reschedule.");
            }

            // TODO: subtract numOfDays from user's days attribute
            // and subtract user's budget
            user.setBudget(user.getBudget().subtract(c.getFee()));
            user.setDays(user.getDays() - numOfDays);

            userRepository.save(user);
            application.setUser(user);
        
        }
        applicationRepository.save(application);
       
    }

    boolean checkWorkingDay(LocalDate d) {
        return (d.getDayOfWeek() == DayOfWeek.SATURDAY || 
                d.getDayOfWeek() == DayOfWeek.SUNDAY ||
                holidayRepository.existsByHolidayDate(d));
    }


}
