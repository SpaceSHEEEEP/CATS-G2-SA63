package sg.edu.iss.cats.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.iss.cats.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

}
