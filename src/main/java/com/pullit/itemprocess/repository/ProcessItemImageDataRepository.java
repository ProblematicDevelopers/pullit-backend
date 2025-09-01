package com.pullit.itemprocess.repository;

import com.pullit.itemprocess.entity.ProcessItemImageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessItemImageDataRepository extends JpaRepository<ProcessItemImageData, Long> {
    void deleteByItemMetadata_ItemId(Long itemId);

}