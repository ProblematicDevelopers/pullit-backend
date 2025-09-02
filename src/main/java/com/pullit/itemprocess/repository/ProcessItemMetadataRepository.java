package com.pullit.itemprocess.repository;

import com.pullit.itemprocess.entity.ProcessItemMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessItemMetadataRepository extends JpaRepository<ProcessItemMetadata, Long> {
    Optional<ProcessItemMetadata> findBySourceItemId(Long sourceItemId);
}