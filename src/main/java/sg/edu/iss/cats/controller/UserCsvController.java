package sg.edu.iss.cats.controller;

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//imports for csv
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

//import for email
import sg.edu.iss.cats.service.EmailService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;


import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.dto.UserDTO;

@RestController
@RequestMapping("/api")
public class UserCsvController {

	@Autowired
	private UserRepository userRepository;
	private EmailService emailService;
	
	public UserCsvController(UserRepository userRepository, EmailService emailService) {
		this.userRepository = userRepository;
		this.emailService = emailService;
	}

	private String generateUsersCsv() throws Exception {

		List<User> users = userRepository.findAll();
			
		List<UserDTO> userDTOs = new ArrayList<>();
			
		for (User user : users) {

		    String managerName = null;

		    if (user.getManager() != null) {
		        managerName = user.getManager().getName();
		    }

		    UserDTO dto = new UserDTO(
		            user.getUserId(),
		            user.getName(),
		            user.isActive(),
		            user.isAdmin(),
		            user.getEmail(),
		            managerName
		    );

		    userDTOs.add(dto);
		}
		
		//special csv classes
		CsvMapper csvMapper = new CsvMapper();
		
		CsvSchema schema = csvMapper
				.schemaFor(UserDTO.class)
		        .withHeader();
		
		return csvMapper
				.writer(schema)					// Apply CSV formatting rules
		        .writeValueAsString(userDTOs);	// Convert data into CSV string
	}
	
	@GetMapping("/allUsers")
	//ResponseEntity is a special Spring Class for defining status, headers, and body
	public ResponseEntity<String> getAllUsers() throws Exception{		
		    
	//better alternative is to use data type byte[] instead of String. but chose byte for simplicity
	String csvFileString = generateUsersCsv();
		    
	// Return the CSV data as a downloadable HTTP response
	return ResponseEntity
			.ok()	// .ok is a http status update = Request Successful
		    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"AllUsers.csv\"")	// Tell browser to download file                             
		    .contentType(MediaType.parseMediaType("text/csv")) // Specify CSV content type
		    .body(csvFileString);	// Send CSV bytes to browser
	}
	
	@PostMapping("/emailUsers")
	public ResponseEntity<String> emailUsers(
//			@RequestParam(name = "Admin Sender") String senderEmail,
			@RequestParam(name = "E-Mail Recipient") String recipient,
			HttpSession session) throws Exception {

		User user = (User) session.getAttribute("user");

		String senderEmail = user.getEmail();
		
	    String csvFileString = generateUsersCsv();

	    emailService.sendEmailWithAttachment(senderEmail,
	            recipient,
	            "CATS - User Report",
	            "Please find attached the latest user report.",
	            csvFileString
	    );
	    
	    return ResponseEntity.ok("Email sent successfully!");
	}
		
}
