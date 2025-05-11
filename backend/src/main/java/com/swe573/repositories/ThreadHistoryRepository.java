package com.swe573.repositories;

import com.swe573.models.ThreadHistoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreadHistoryRepository extends JpaRepository<ThreadHistoryEntry, Long> {
    
    List<ThreadHistoryEntry> findByThreadIdOrderByCreatedAtDesc(Long threadId);
    
    Page<ThreadHistoryEntry> findByThreadIdOrderByCreatedAtDesc(Long threadId, Pageable pageable);
    
    @Query("SELECT h FROM ThreadHistoryEntry h WHERE h.thread.id = :threadId AND h.entityType = :entityType ORDER BY h.createdAt DESC")
    List<ThreadHistoryEntry> findByThreadIdAndEntityTypeOrderByCreatedAtDesc(
        @Param("threadId") Long threadId, 
        @Param("entityType") ThreadHistoryEntry.EntityType entityType
    );
    
    @Query("SELECT h FROM ThreadHistoryEntry h WHERE h.thread.id = :threadId AND h.entityId = :entityId AND h.entityType = :entityType ORDER BY h.createdAt DESC")
    List<ThreadHistoryEntry> findByThreadIdAndEntityIdAndEntityTypeOrderByCreatedAtDesc(
        @Param("threadId") Long threadId, 
        @Param("entityId") Long entityId,
        @Param("entityType") ThreadHistoryEntry.EntityType entityType
    );
    
    @Query("SELECT h FROM ThreadHistoryEntry h WHERE h.user.id = :userId ORDER BY h.createdAt DESC")
    List<ThreadHistoryEntry> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
} 