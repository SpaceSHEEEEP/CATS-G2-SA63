package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.iss.cats.model.User;
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
    @DisplayName("Seeded credentials use a BCrypt hash, never plaintext")
    void seededPasswordIsEncoded() {
        User user = userRepository.findByUsername("bobby").orElseThrow();
        assertThat(user.getPassword()).startsWith("$2");
        assertThat(user.getPassword()).isNotEqualTo("pw");
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

	@Test
	@DisplayName("Returns User Object when the username exists")
	void findUserByUsername() {
        User user = userRepository.findByUsername("bobby").orElseThrow();

	    assertThat(user).isNotNull();
	    assertThat(user.getUsername()).isEqualTo("bobby");
	}

	@Test
	@DisplayName("Does not return any User Object when the username does not exist")
	void findUserByUsernameDoesNotExist() {
        assertThat(userRepository.findByUsername("mr.brightside")).isEmpty();
	}

	@Test
	@DisplayName("Returns TRUE when a user is a manager")
	void findUserIsManager() {
	    boolean isManager = userRepository.existsByManager_UserId(2);

	    assertThat(isManager).isTrue();
	}

	@Test
	@DisplayName("Returns False when a user is not manager")
	void findUserIsManagerDoesNotExist() {
	    boolean isManager = userRepository.existsByManager_UserId(5);

	    assertThat(isManager).isFalse();
	}

}
