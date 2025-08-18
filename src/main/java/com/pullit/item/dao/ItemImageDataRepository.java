package com.pullit.item.dao;

import com.pullit.item.entity.ItemImageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemImageDataRepository extends JpaRepository<ItemImageData, Long> {
    Optional<ItemImageData> findByItemId(Long itemId);
}
