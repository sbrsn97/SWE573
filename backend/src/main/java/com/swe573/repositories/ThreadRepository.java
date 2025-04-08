package com.swe573.repositories;

import com.swe573.models.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThreadRepository extends JpaRepository<Thread, Long> {
    List<Thread> findByAuthorId(Long authorId);
    
    @Query("SELECT t FROM Thread t JOIN t.tags tag WHERE tag.label = :tagLabel")
    List<Thread> findByTagLabel(@Param("tagLabel") String tagLabel);
    
    @Query("SELECT t FROM Thread t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Thread> searchThreads(@Param("keyword") String keyword);

    @Query("SELECT t FROM Thread t JOIN t.threadFollowers f WHERE f.id = :userId")
    List<Thread> findThreadsFollowedByUser(@Param("userId") Long userId);
} 