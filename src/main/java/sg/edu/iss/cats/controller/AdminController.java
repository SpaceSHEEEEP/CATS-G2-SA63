package sg.edu.iss.cats.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import sg.edu.iss.cats.dto.CourseApplicantSummary;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.LoginForm;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;

import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.HolidayRepository;

@Controller
@RequestMapping("/admin")
public class AdminController {
	
    private final int pageSize = 10;
	private final UserRepository userRepository;
	private final HolidayRepository holidayRepository;
	private final CourseRepository courseRepository;
	
	public AdminController(UserRepository userRepository, HolidayRepository holidayRepository, CourseRepository courseRepository) {
		this.userRepository = userRepository;
		this.holidayRepository = holidayRepository;
		this.courseRepository = courseRepository;

	}

	@GetMapping("/")
	public String showAdmin(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "pageNum", defaultValue = "0") int pageNum,
            Model model) {

        if (user == null)                       return "redirect:/admin/login";
        if (user.getRole() != Role.ROLE_ADMIN) return "redirect:/staff/index";

        // Load current user records
        model.addAttribute("users", userRepository.findAll());
        
        // Show holidays for the current calendar year only.
        int year = LocalDate.now().getYear();
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);

        model.addAttribute("holidayYear", year);
        model.addAttribute("holidays",
                holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(firstDay, lastDay));

        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // TODO: get pages WITH application count!! The one below doesn't have application count
        // Page<Course> courseSummaries = courseRepository.findAllByOrderByApplicationsSizeDesc(pageable);
        // model.addAttribute("courseSummaries", courseSummaries);
        Page<CourseApplicantSummary> courseSummaries = courseRepository.findCoursesWithApplicantCounts(pageable);
        model.addAttribute("courseSummaries", courseSummaries);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageNumLast", courseSummaries.getTotalPages());
        model.addAttribute("user", userRepository.findById(user.getUserId()).orElseThrow());
        
        return "admin";
	}
	
	@GetMapping("/login")
	public String showAdminLogin(Model model) {
		model.addAttribute("login", new LoginForm());
		model.addAttribute("adminLogin", true); // TODO: check what this is for
		return "login";
	}
	
    @GetMapping("/createuser")
    public String createUser(
            @AuthenticationPrincipal User user,
            Model model) {
        if (user == null) return "redirect:/admin/login";
        if (!user.isAdmin()) return "redirect:/staff/index";

        model.addAttribute("userForm", new User());

        // return all managers in the current user repository
        List<User> managers = userRepository.findAllManagers();
        model.addAttribute("managers", managers);

        return "userform";
    }

    @PostMapping("/createuser")
    public String saveUser(
            @AuthenticationPrincipal User user,
            @Valid @ModelAttribute("userForm") User form, 
            BindingResult result, 
            Model model, 
            RedirectAttributes ra) {

        if (user == null) return "redirect:/admin/login";
        if (!user.isAdmin()) return "redirect:/staff/index";

        // during registration of new users,
        if (form.getUserId() == null){ 
            // check if the username already exists
            if (userRepository.existsByUsername(form.getUsername())){
            result.rejectValue("username", "usernameerror", "Username already exists");
            }
        }
        // during editing of existing users,
        else {
            // 'Not' is to exclude ownself from the check of duplicate username, allowing edting and updating to pass through
            // Example when the admin is trying to edit Bob's details, it will pass even though the username already exists, just because it is his to begin with

            // If the username is found to exist within a user's details when editing another user's details, reject
            if (userRepository.existsByUsernameAndUserIdNot(form.getUsername(), form.getUserId())){
                result.rejectValue("username", "usernameerror", "Username already exists");
            }
        }
    
        if (result.hasErrors()) {
            // repopulate the manager list
            List<User> managers = userRepository.findAllManagers();
            model.addAttribute("managers", managers);
            return "userform";
        }

        // Check if it is a new user or an existing user
        boolean isNewUser = (form.getUserId()==null);
        
        // retrieve back fields that are not included within the form, to prevent resetting of values for example credentials like pwd and budgeted fields
        // wanted to test a case whereby admin assigns more administrators, log out of admin and tried to sign in to the assignee's account
        // the assignee was found to not being able to log in
        // hence, this code
        if (!isNewUser) {
            User originalUserDetails = userRepository.findById(form.getUserId()).orElse(null);
            form.setPassword(originalUserDetails.getPassword());
            // set budgeted to be same as the actual as per the user form before saving
            form.setBudgetedAllowance(form.getActualAllowance());
            form.setBudgetedDays(form.getActualDays());
        }

        userRepository.save(form);

        // Flash Attribute for success message
        if (isNewUser) {
            ra.addFlashAttribute("successmsg", "User '" + form.getUserId() + "' created successfully!");
        } else {
            ra.addFlashAttribute("successmsg", "User '" + form.getUserId() + "' updated successfully!");
        }

        return "redirect:/admin";
    }

    @GetMapping("/edituser")
    public String editUser(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "userId", required = true) Integer id,
            Model model,
            RedirectAttributes ra) {

        // check if user is logged in
        if (user == null) return "redirect:/admin/login";
        if (!user.isAdmin()) return "redirect:/staff/index";

        // check if the user to be edited exists
        User editedUser = userRepository.findById(id).orElse(null);
        if (editedUser == null) return "redirect:/admin";

        // Prefill the form
        model.addAttribute("userForm", editedUser);

        // Prefill manager list
        List<User> managers = userRepository.findAllManagers();

        model.addAttribute("managers", managers);
        return "userform";
    }

    @PostMapping("/deleteuser")
    public String deleteUser(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "userId", required = true) Integer id,
            Model model,
            RedirectAttributes ra){
        
        // check if user is logged in
        if (user == null) return "redirect:/admin/login";
        if (!user.isAdmin()) return "redirect:/staff/index";

        // check if admin is trying to delete himself/herself
        if (user.getUserId().equals(id)){
            ra.addFlashAttribute("errormsg", "You are not authorised to delete yourself.");
            return "redirect:/admin";
        }

        // check if the user to be deleted is the CEO
        if (id == 1) {
            ra.addFlashAttribute("errormsg", "You are not authorised to remove this person.");
            return "redirect:/admin";
        }

        // check if the user to be deleted exists
        User deletedUser = userRepository.findById(id).orElse(null);
        if (deletedUser == null) return "redirect:/admin";

        // check if the user to be deleted is a manager
        if (!deletedUser.getSubordinates().isEmpty()) {
            // for every subordinate in the list of deleted user's subordinates
            for (User s : deletedUser.getSubordinates()) {
                // remove manager
                s.setManager(null);
                userRepository.save(s);
            }
        }

        // remove his/her manager if he/she reports to a superior
        deletedUser.setManager(null);
        
        userRepository.delete(deletedUser);

        // flash attribute
        ra.addFlashAttribute("successmsg", "User '" + deletedUser.getUserId() + "' deleted successfully!");

        return "redirect:/admin";
    }
}
