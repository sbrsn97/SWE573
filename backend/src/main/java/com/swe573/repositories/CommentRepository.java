package com.swe573.repositories;

import com.swe573.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByThreadIdOrderByCreatedAt(Long threadId);
    List<Comment> findByThreadIdAndParentIsNullOrderByCreatedAt(Long threadId);
    List<Comment> findByParentIdOrderByCreatedAt(Long parentId);
    List<Comment> findByAuthorIdOrderByCreatedAt(Long authorId);
} 