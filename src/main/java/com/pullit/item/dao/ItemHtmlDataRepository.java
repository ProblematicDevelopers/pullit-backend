package com.pullit.item.dao;

import com.pullit.item.entity.ItemHtmlData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemHtmlDataRepository extends JpaRepository<ItemHtmlData, Long> {
    Optional<ItemHtmlData> findByItemId(Long itemId);
}
