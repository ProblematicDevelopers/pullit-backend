package com.pullit.itemprocess.repository;

import com.pullit.itemprocess.entity.ProcessItemHtmlData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessItemHtmlDataRepository extends JpaRepository<ProcessItemHtmlData, Long> {
    void deleteByItemMetadata_ItemId(Long itemId);

}