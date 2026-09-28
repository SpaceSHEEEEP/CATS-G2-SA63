package sg.edu.iss.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import sg.edu.iss.cats.model.Application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/staff")
public class ApplicationController {


    @PostMapping("/submitapplication")
    public String submitApplication(@Valid Application application, BindingResult result) {
        // Check for valid annotations in the Application, Course Model.
        if (result.hasErrors()) {
            return "applicationform";
        }
        // Apply business validation rules in the service layer.

        return "redirect:/staff/index.html";
}
}
