package sg.edu.iss.cats.repository;

import sg.edu.iss.cats.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    public boolean existsByUsername(String username);
    public boolean existsByUsernameAndPassword(String username, String password);
    
    // for Application Repo 
    public User findByUsername(String username);

    // A user is a manager if another employee reports to them.
    boolean existsByManager_UserId(Integer managerId);
    
}
