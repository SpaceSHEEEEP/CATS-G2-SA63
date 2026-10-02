package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.iss.cats.repository.UserRepository;

//Uses sample users from data.sql and the test-profile MySQL database.
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTests {
	
	@Autowired
	private UserRepository userRepository;
	
	@Test
	@DisplayName("Returns TRUE when Bobby's username exists")
	void bobbyExists() {
	    boolean exists = userRepository.existsByUsername("bobby");

	    assertThat(exists).isTrue();
	}
	
	@Test
	@DisplayName("Returns true when Bobby's password matches")
	void bobbyPasswordMatches() {
	    boolean matches = userRepository.existsByUsernameAndPassword(
	            "bobby", "pw");

	    assertThat(matches).isTrue();
	}
	
	@Test
	@DisplayName("FALSE when Bobby's pass dont matches")
	void bobbyPasswordNotMatch( ) {
		boolean matches = userRepository.existsByUsernameAndPassword(
				"bobby", "wrong");
		assertThat(matches).isFalse();
	}
	
	@Test
	@DisplayName("FALSE when the username does not exist")
	void unknownUsernameDoesNotExist() {
	    boolean exists = userRepository.existsByUsername("mr.brightside");

	    assertThat(exists).isFalse();
	}
	
	@Test
	@DisplayName("FALSE when the username does not exist but real password")
	void unknownUsernameExistsPassword() {
	    boolean exists = userRepository.existsByUsernameAndPassword(
	    		"mr.brightside", "password");

	    assertThat(exists).isFalse();
	}

}
