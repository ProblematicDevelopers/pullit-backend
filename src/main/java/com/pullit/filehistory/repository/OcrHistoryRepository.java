package com.pullit.filehistory.repository;

import com.pullit.filehistory.entity.OcrHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcrHistoryRepository extends JpaRepository<OcrHistory, Long> {
}