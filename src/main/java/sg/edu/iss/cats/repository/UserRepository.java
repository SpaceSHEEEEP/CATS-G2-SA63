package sg.edu.iss.cats.repository;

import sg.edu.iss.cats.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    public boolean existsByUsername(String username);
    public boolean existsByUsernameAndPassword(String username, String password);

    
}
