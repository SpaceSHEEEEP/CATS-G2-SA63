package sg.edu.iss.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import sg.edu.iss.cats.model.Application;

import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/staff")
public class ApplicationController {


    @PostMapping("/submitapplication")
    public String submitApplication(@Valid Application application, BindingResult result, Model model) {
        // Check for valid annotations in the Application, Course Model.
        if (result.hasErrors()) {
            model.addAttribute("applicationForm", new Application());
            return "applicationform";
        }
        // Apply business validation rules in the service layer.

        return "redirect:/staff/";
}
}
