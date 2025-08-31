package com.pullit.itemprocess.repository;

import com.pullit.itemprocess.entity.ProcessedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedItemRepository extends JpaRepository<ProcessedItem, Long> {
}