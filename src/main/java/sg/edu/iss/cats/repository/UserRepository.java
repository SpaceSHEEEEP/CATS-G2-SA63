package sg.edu.iss.cats.repository;

import sg.edu.iss.cats.model.User;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    public boolean existsByUsername(String username);
    // for user editing by admin
    public boolean existsByUsernameAndUserIdNot(String username, Integer userId);
    public boolean existsByUsernameAndPassword(String username, String password);
    // below can be used to find user with username, then get user's password from here
    // public Optional<User> findByUsername(String username);
    // use this instead of below for more safety
    
    // for Application Repo 
    public User findByUsername(String username);

    // A user is a manager if another employee reports to them.
    boolean existsByManager_UserId(Integer managerId);

    // Give me a list of managers
    @Query("""
        SELECT m
        FROM User m
        INNER JOIN m.subordinates s
        GROUP BY m.userId, m.name
        ORDER BY m.name ASC   
            """)
    public List<User> findAllManagers();

    // Give me a list of subordinates
    @EntityGraph(attributePaths = {"subordinates"})
    @Query("""
        SELECT DISTINCT s 
        FROM User s 
        WHERE s.manager IS NOT NULL
        ORDER BY s.name ASC        
        """)
    public List<User> findAllSubordinates();


    // Give me a list of subordinates of a manager
    public List<User> findAllByManager_UserId(Integer managerId);
    // can use this instead of above, then use .isEmpty() to check?
    
}
