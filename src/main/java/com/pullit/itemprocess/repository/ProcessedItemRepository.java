package com.pullit.itemprocess.repository;

import com.pullit.itemprocess.entity.ProcessedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedItemRepository extends JpaRepository<ProcessedItem, Long> {
    
    @Query("""
        SELECT p FROM ProcessedItem p
        LEFT JOIN FETCH p.ocrHistories oh
        LEFT JOIN FETCH oh.pdfImage pi
        LEFT JOIN FETCH pi.fileHistory fh
        LEFT JOIN FETCH fh.subject s
        WHERE p.id = :id
        """)
    Optional<ProcessedItem> findForConversion(@Param("id") Long id);
}