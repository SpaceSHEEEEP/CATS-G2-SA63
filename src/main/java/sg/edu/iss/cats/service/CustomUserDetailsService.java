package sg.edu.iss.cats.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

@Service 
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override 
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        sg.edu.iss.cats.model.User user = userRepository.findByUsername(username)     
                                  .orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found"));

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                   .password(user.getPassword())
                   .authorities(user.getRole())
                   .build();

    }

    
}
