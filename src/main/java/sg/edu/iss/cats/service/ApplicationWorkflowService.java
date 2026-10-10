package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;

/** The only service used for state-changing application operations. All checks
 * are repeated server-side on POST and performed inside the transaction. */
@Service
public class ApplicationWorkflowService {
    private final AppRepo applications;
    private final CourseRepository courses;
    private final UserRepository users;
    private final CommentRepository comments;
    private final TrainingAccountService account;

    public ApplicationWorkflowService(AppRepo applications, CourseRepository courses,
            UserRepository users, CommentRepository comments, TrainingAccountService account) {
        this.applications = applications;
        this.courses = courses;
        this.users = users;
        this.comments = comments;
        this.account = account;
    }

    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Singapore")); }

    private User lockedUser(Integer userId) {
        return users.findLockedById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Application ownedApplication(Integer appId, Integer ownerId) {
        Application application = applications.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!application.getUser().getUserId().equals(ownerId)) {
            throw new SecurityException("This application is not yours");
        }
        return application;
    }

    private boolean editable(Status status) {
        return status == Status.APPLIED || status == Status.UPDATED;
    }

    private void validateCourse(Course course) {
        if (course == null || course.getCourseName() == null || course.getCourseName().isBlank()
                || course.getCourseType() == null || course.getStartDate() == null
                || course.getEndDate() == null || course.getDuration() == null
                || course.getFee() == null || course.getFee().signum() < 0
                || course.getTrainingProvider() == null || course.getTrainingProvider().isBlank()
                || course.getLocation() == null || course.getLocation().isBlank()) {
            throw new IllegalArgumentException("All course details, including duration and provider, are required");
        }
        if (course.getStartDate().isAfter(course.getEndDate()))
            throw new IllegalArgumentException("Start date must not follow end date");
        if (!course.getStartDate().isAfter(today()))
            throw new IllegalArgumentException("Start date must be in the future");
        if (!account.isWorkingDay(course.getStartDate()) || !account.isWorkingDay(course.getEndDate()))
            throw new IllegalArgumentException("Course start and end must be working days");
        if (course.getCourseType() == CourseType.INTERNAL && course.getFee().compareTo(BigDecimal.ZERO) != 0)
            throw new IllegalArgumentException("Internal courses must have zero fee");
        if (course.getCourseType() != CourseType.INTERNAL && course.getDuration() != CourseDuration.FULLDAY)
            throw new IllegalArgumentException("Only internal training may be half-day");
        if (course.getDuration() != CourseDuration.FULLDAY
                && !course.getStartDate().equals(course.getEndDate()))
            throw new IllegalArgumentException("A half-day session must have the same start and end date");
        if (account.workingDays(course) <= 0)
            throw new IllegalArgumentException("The course has no training days");
    }

    private boolean sameCourse(Course a, Course b) {
        return a.getCourseName().equals(b.getCourseName())
                && a.getCourseType() == b.getCourseType()
                && a.getStartDate().equals(b.getStartDate())
                && a.getEndDate().equals(b.getEndDate())
                && a.getFee().compareTo(b.getFee()) == 0
                && a.getLocation().equals(b.getLocation())
                && a.getTrainingProvider().equals(b.getTrainingProvider())
                && a.getDuration() == b.getDuration();
    }

    private Course copyCourse(Course input) {
        return new Course(input.getCourseName(), input.getCourseType(),
                input.getStartDate(), input.getEndDate(), input.getLocation(),
                input.getTrainingProvider(), input.getFee(), input.getDuration());
    }

    private Course resolveCourse(Course input, Application existing) {
        if (input.getCourseId() == null) return courses.save(copyCourse(input));
        Course stored = courses.findById(input.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course no longer exists"));
        if (sameCourse(stored, input)) {
            if (Boolean.TRUE.equals(stored.getArchived()) && existing == null)
                throw new IllegalArgumentException("Course is no longer in the catalogue");
            return stored;
        }
        // Editing a custom application is copy-on-write: never rewrite a course
        // that could already be referenced by another employee's application.
        if (existing != null && existing.getCourse().getCourseId().equals(stored.getCourseId()))
            return courses.save(copyCourse(input));
        throw new IllegalArgumentException("Selected course details cannot be changed");
    }

    private boolean overlap(Course a, Course b) {
        LocalDate start = a.getStartDate().isAfter(b.getStartDate()) ? a.getStartDate() : b.getStartDate();
        LocalDate end = a.getEndDate().isBefore(b.getEndDate()) ? a.getEndDate() : b.getEndDate();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!account.isWorkingDay(date)) continue;
            boolean aAm = a.getDuration() == CourseDuration.HALFDAYAM;
            boolean aPm = a.getDuration() == CourseDuration.HALFDAYPM;
            boolean bAm = b.getDuration() == CourseDuration.HALFDAYAM;
            boolean bPm = b.getDuration() == CourseDuration.HALFDAYPM;
            if ((aAm && bPm) || (aPm && bAm)) continue;
            return true;
        }
        return false;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void saveApplication(Application request, Integer userId) {
        User user = lockedUser(userId);
        account.allocation(user, today().getYear());
        Application original = null;
        if (request.getId() != null) {
            original = ownedApplication(request.getId(), userId);
            if (!editable(original.getStatus()))
                throw new IllegalArgumentException("Only applied or updated applications may be edited");
        }
        if (request.getUserReason() == null || request.getUserReason().isBlank()
                || request.getUserReason().length() > 1000)
            throw new IllegalArgumentException("A justification of at most 1,000 characters is required");
        Course incoming = request.getCourse();
        validateCourse(incoming);
        account.validateCapacity(user, incoming, original == null ? null : original.getId());
        for (Application another : applications.findByUser_UserIdAndStatusIn(userId,
                List.of(Status.APPLIED, Status.UPDATED, Status.APPROVED))) {
            if (original != null && another.getId().equals(original.getId())) continue;
            if (overlap(incoming, another.getCourse()))
                throw new IllegalArgumentException("This course overlaps with another active application");
        }
        Course savedCourse = resolveCourse(incoming, original);
        Application target = original == null ? new Application() : original;
        target.setUser(user); // NEVER take the owner from the submitted form.
        target.setCourse(savedCourse);
        target.setUserReason(request.getUserReason().trim());
        target.setStatus(original == null ? Status.APPLIED : Status.UPDATED);
        applications.saveAndFlush(target);
        request.setId(target.getId());
        account.syncLegacyCurrentYear(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteApplication(Application request, Integer userId) {
        User user = lockedUser(userId);
        account.allocation(user, today().getYear());
        Application target = ownedApplication(request.getId(), userId);
        if (!editable(target.getStatus()))
            throw new IllegalArgumentException("Only applied or updated applications may be deleted");
        target.setStatus(Status.DELETED);
        applications.saveAndFlush(target);
        account.syncLegacyCurrentYear(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelApplication(Integer appId, Integer userId) {
        User user = lockedUser(userId);
        account.allocation(user, today().getYear());
        Application target = ownedApplication(appId, userId);
        if (target.getStatus() != Status.APPROVED)
            throw new IllegalArgumentException("Only approved courses may be cancelled");
        target.setStatus(Status.CANCELLED);
        applications.saveAndFlush(target);
        account.syncLegacyCurrentYear(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void completeApplication(Integer appId, Integer userId, String commentText) {
        User user = lockedUser(userId);
        account.allocation(user, today().getYear());
        Application target = ownedApplication(appId, userId);
        if (target.getStatus() != Status.APPROVED)
            throw new IllegalArgumentException("Only approved courses may be completed");
        if (!target.getCourse().getEndDate().isBefore(today()))
            throw new IllegalArgumentException("The course must have ended before completion");
        if (commentText == null || commentText.isBlank() || commentText.length() > 1000)
            throw new IllegalArgumentException("An experience comment of at most 1,000 characters is required");
        Comment comment = target.getComment();
        if (comment == null) comment = new Comment(target);
        comment.setCommentText(commentText.trim());
        target.setStatus(Status.COMPLETED);
        applications.saveAndFlush(target);
        comments.save(comment); // same transaction as status update.
        account.syncLegacyCurrentYear(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decide(Integer managerId, Integer applicationId, Status decision, String reason) {
        User manager = lockedUser(managerId);
        if (manager.getRole() != Role.ROLE_MANAGER)
            throw new SecurityException("Only managers may approve or reject applications");
        if (decision != Status.APPROVED && decision != Status.REJECTED)
            throw new IllegalArgumentException("Invalid manager decision");
        if (reason == null || reason.isBlank() || reason.length() > 1000)
            throw new IllegalArgumentException("A decision reason of at most 1,000 characters is required");
        Application target = applications.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        User owner = target.getUser();
        account.allocation(owner, today().getYear());
        if (owner.getManager() == null || !owner.getManager().getUserId().equals(managerId))
            throw new SecurityException("Application does not belong to your direct subordinate");
        if (!editable(target.getStatus()))
            throw new IllegalArgumentException("This application is no longer pending");
        target.setManagerReason(reason.trim());
        target.setStatus(decision);
        applications.saveAndFlush(target);
        account.syncLegacyCurrentYear(owner);
    }
}
