package sg.edu.iss.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff") // for employee's stuff
public class EmployeeController {
    @GetMapping("/form")
    public String showForm(){
        return "applicationform";
    }
    
}
