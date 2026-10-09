package sg.edu.iss.cats.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseDuration;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.HolidayRepository;
import sg.edu.iss.cats.repository.UserRepository;

@Service
public class ApplicationService {

    private final AppRepo appRepo;
    private final CourseRepository courseRepository;
    private final HolidayRepository holidayRepository;
    private final UserRepository userRepository;

    public ApplicationService(
            AppRepo appRepo, 
            CourseRepository courseRepository,
            HolidayRepository holidayRepository,
            UserRepository userRepository) {
        this.appRepo = appRepo;
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
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the application is new or edit
        Integer appId = application.getId();
        boolean editApplication = (appId != null && appRepo.existsById(appId));

        // if the application is being edited,
        if (editApplication){
            // Get the original application
            Application originalApplication = appRepo.findById(appId).orElseThrow(() -> new RuntimeException("Application not found"));

            // Check if the application is already deleted
            if (originalApplication.getStatus() == Status.DELETED) 
            throw new RuntimeException("Cannot edit a deleted application");

            // Refund the days and budget back to user first, to deduct again before the user re-saves.
            double retrieveNumOfDays = calculateNumOfDays(originalApplication.getCourse());
            user.setBudgetedAllowance(user.getBudgetedAllowance().add(originalApplication.getCourse().getFee()));
            user.setBudgetedDays(user.getBudgetedDays() + retrieveNumOfDays);
        }

        // ensure that the course is either in the course sql table or it will be added into the sql table
        // if editing application, course details might change. just add into course table, for simplicity
        Course c = application.getCourse();
        if (c.getCourseId() == null || 
            !courseRepository.existsById(c.getCourseId()) ||
            (editApplication && c.getCourseType() != CourseType.INTERNAL)) {
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
        // start date must be before end date
        if (c.getStartDate().isAfter(c.getEndDate())) 
            throw new RuntimeException("Start date must be before end date");
        
        // start date must be in the future
        if (!c.getStartDate().isAfter(LocalDate.now())) 
            throw new RuntimeException("Start date must be in the future");

        // half day sessions are only for internal training 
        if (c.getCourseType() != CourseType.INTERNAL && c.getDuration() != CourseDuration.FULLDAY) 
            throw new RuntimeException("Only internal courses can be half day");

        // start and end dates must be working days             
        if (!isWorkingDay(c.getStartDate()))
            throw new RuntimeException("Start day must be a working day");
        if (!isWorkingDay(c.getEndDate()))
            throw new RuntimeException("End day must be a working day");

        // count the number of working days
        // changed this to a function
        double numOfDays = calculateNumOfDays(c);
       
        // user must have enough budget 
        if (c.getFee().compareTo(user.getBudgetedAllowance()) > 0) 
            throw new RuntimeException("You do not have enough budget to apply to this course");
        // user must have enough days
        if (numOfDays > user.getBudgetedDays()) 
            throw new RuntimeException("You do not have enough days to apply to this course");

        // The course period must not overlap with another ‘Applied’, ‘Updated’ 
        // or ‘Approved’ course application of the same employee. (to be done after added manager features)
        List<Application> applications = appRepo.findByUser_UserIdAndStatusIn(userId, List.of(Status.UPDATED, Status.APPROVED));  

        for (Application a : applications) {
            if (editApplication && a.getId().equals(appId)) continue;
            if (c.getStartDate().compareTo(a.getCourse().getEndDate()) <= 0 &&
                c.getEndDate().compareTo(a.getCourse().getStartDate())  >= 0)
                throw new RuntimeException("This course overlaps with your " + a.getCourse().getCourseName() + " course. Please reschedule.");
        }

        // Subtract user's budgeted statistics
        user.setBudgetedAllowance(user.getBudgetedAllowance().subtract(c.getFee()));
        user.setBudgetedDays(user.getBudgetedDays() - numOfDays);

        // Additional Rule: Budgeted should always be lower or equal to Actual
        // Gave it a thought and felt that there won't be a scenario when budgeted > actual
        if (user.getBudgetedAllowance().compareTo(user.getActualAllowance())>0)
            {throw new RuntimeException("Budgeted Allowance should not be higher than Actual Allowance.");}

        if (user.getBudgetedDays().compareTo(user.getActualDays())>0)
            {throw new RuntimeException("Budgeted Days should not be higher than Actual Days.");}

        userRepository.save(user);
        application.setUser(user);

        
        if (editApplication) application.setStatus(Status.UPDATED);
        else application.setStatus(Status.APPLIED);
        appRepo.save(application);
       
    }

    @Transactional( 
    propagation  = Propagation.REQUIRED,
    isolation    = Isolation.SERIALIZABLE,
    rollbackFor  = Exception.class,
    timeout      = 30
    )
    public void deleteApplication(Application application, Integer userId) {
        // Obtain user object
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
  
        // checks if the application exists in the database
        Application originalApplication = appRepo.findById(application.getId()).orElseThrow(() -> new RuntimeException("Application not found"));

        // prevents deletion of an application that is already deleted
        if (originalApplication.getStatus() == Status.DELETED)
            throw new RuntimeException("Cannot delete an application that is already deleted");

        // set status
        originalApplication.setStatus(Status.DELETED);
        
        // refund
        double retrieveNumOfDays = calculateNumOfDays(originalApplication.getCourse());
        user.setBudgetedAllowance(user.getBudgetedAllowance().add(originalApplication.getCourse().getFee()));
        user.setBudgetedDays(user.getBudgetedDays() + retrieveNumOfDays);

        userRepository.save(user);
        appRepo.save(originalApplication);
    }

    @Transactional( 
    propagation  = Propagation.REQUIRED,
    isolation    = Isolation.SERIALIZABLE,
    rollbackFor  = Exception.class,
    timeout      = 30
    )
    public void completeApplication(Application application, Integer userId) {
        // Obtain user object
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // checks if the application exists in the database
        Application originalApplication = appRepo.findById(application.getId()).orElseThrow(() -> new RuntimeException("Application not found"));

        Course c = originalApplication.getCourse();
        double numOfDays = calculateNumOfDays(c);

        // subtract the actual statistics
        user.setActualAllowance(user.getActualAllowance().subtract(c.getFee()));
        user.setActualDays(user.getActualDays() - numOfDays);
        
        // set status
        originalApplication.setStatus(Status.COMPLETED);

        userRepository.save(user);
        appRepo.save(originalApplication);
    }

    // converted into a function to be used multiple times in saveApplication and deleteApplication functions
    double calculateNumOfDays(Course c){
        double numOfDays = 0.0;
        if (c.getDuration() != CourseDuration.FULLDAY) return numOfDays = 0.5;
        else {
            LocalDate start = c.getStartDate();
            LocalDate end = c.getEndDate();
            do {
                if (isWorkingDay(start)) numOfDays++;
                start = start.plusDays(1);
            } while (!start.isAfter(end)); // your original code will exclude the end date, so changed a bit here.
        }
        return numOfDays;        
    }

    boolean isWorkingDay(LocalDate d) {
        return !(d.getDayOfWeek() == DayOfWeek.SATURDAY || 
                d.getDayOfWeek() == DayOfWeek.SUNDAY ||
                holidayRepository.existsByHolidayDate(d));
    }

}
