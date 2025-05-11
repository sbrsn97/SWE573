package com.swe573.repositories;

import com.swe573.models.Vote;
import com.swe573.models.enums.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByUserIdAndThreadId(Long userId, Long threadId);
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    Optional<Vote> findByUserIdAndThreadId(Long userId, Long threadId);
    Optional<Vote> findByUserIdAndCommentId(Long userId, Long commentId);
    
    // Count votes by thread ID and vote type
    long countByThreadIdAndType(Long threadId, VoteType type);
    
    // Count votes by comment ID and vote type
    long countByCommentIdAndType(Long commentId, VoteType type);
} 