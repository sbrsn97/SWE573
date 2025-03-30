package com.swe573.repositories;

import com.swe573.models.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByUserIdAndThreadId(Long userId, Long threadId);
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    Optional<Vote> findByUserIdAndThreadId(Long userId, Long threadId);
    Optional<Vote> findByUserIdAndCommentId(Long userId, Long commentId);
} 