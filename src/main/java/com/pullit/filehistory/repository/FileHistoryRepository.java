package com.pullit.filehistory.repository;

import com.pullit.filehistory.entity.FileHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileHistoryRepository extends JpaRepository<FileHistory, Long> {
    
    Page<FileHistory> findByCreatedByOrderByCreatedDateDesc(String createdBy, Pageable pageable);
    
    FileHistory findByIdAndCreatedBy(Long id, String createdBy);
}
