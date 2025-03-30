package com.swe573.repositories;

import com.swe573.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByLabel(String label);
    
    Optional<Tag> findByWikidataEntityId(String wikidataEntityId);
    
    @Query("SELECT t FROM Tag t WHERE t.label LIKE %:keyword% OR t.description LIKE %:keyword%")
    List<Tag> searchTags(@Param("keyword") String keyword);
    
    boolean existsByLabel(String label);
    
    boolean existsByWikidataEntityId(String wikidataEntityId);
} 