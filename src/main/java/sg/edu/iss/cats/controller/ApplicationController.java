package sg.edu.iss.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Course;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/staff")
public class ApplicationController {

    // Moved to EmployeeController.java
    /*
    @PostMapping("/submitapplication")
    public String submitApplication(@Valid @ModelAttribute("applicationForm") Application applicationForm, BindingResult result, Model model) {
        // Check for valid annotations in the Application, Course Model.
        if (result.hasErrors()) {
            // model.addAttribute("applicationForm", new Application());
            return "applicationform";
        }
        // Apply business validation rules in the service layer.

        return "redirect:/staff/index";
    }
    */

	@GetMapping("/apply")
	// public String showApplicationForm(@PathVariable int courseId, Model model) {
	public String showApplicationForm(
            @RequestParam(name = "courseId", required = false) Integer courseId, 
            HttpSession session,
            Model model) 
    {
        if (session.getAttribute("user") != null) {
            // TODO: display user's details
        }

        if (courseId != null) {
            // TODO: prefill course's details, applicable only for INTERNAL courses
            Course course = courseRepository.findById(courseId).orElse(null);
            model.addAttribute("course", course);
        } 

		model.addAttribute("applicationForm", new Application());
	    return "applicationform";
	    
	    // TODO: add invalid id handling too
    }
    
    @PostMapping("/submitapplication")
    public String submitApplication(
            @Valid @ModelAttribute("applicationForm") Application applicationForm, 
            BindingResult result, 
            Model model) {
        // Check for valid annotations in the Application, Course Model.
        if (result.hasErrors()) {
            // model.addAttribute("applicationForm", new Application());
            return "applicationform";
        }
        // Apply business validation rules in the service layer.

        return "redirect:/staff/index";
    }
}
