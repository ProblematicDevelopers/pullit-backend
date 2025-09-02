package com.pullit.exam.repository;

import com.pullit.exam.entity.UserExamItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 시험 문제 Repository
 */
@Repository
public interface UserExamItemRepository extends JpaRepository<UserExamItem, Long> {
    
    /**
     * 사용자 시험의 모든 문제 조회
     */
    List<UserExamItem> findByUserExamIdOrderByItemOrder(Long userExamId);
    
    /**
     * 사용자 시험의 특정 순서 문제 조회
     */
    Optional<UserExamItem> findByUserExamIdAndItemOrder(Long userExamId, Integer itemOrder);
    
    /**
     * 사용자 시험의 문제 개수 조회
     */
    @Query("SELECT COUNT(uei) FROM UserExamItem uei WHERE uei.userExam.id = :userExamId")
    long countByUserExamId(@Param("userExamId") Long userExamId);
    
    /**
     * 사용자 시험의 총 점수 조회
     */
    @Query("SELECT SUM(uei.points) FROM UserExamItem uei WHERE uei.userExam.id = :userExamId")
    Integer getTotalPointsByUserExamId(@Param("userExamId") Long userExamId);
    
    /**
     * 특정 문제를 포함하는 사용자 시험 목록 조회
     */
    @Query("SELECT DISTINCT uei.userExam.id FROM UserExamItem uei WHERE uei.itemId = :itemId")
    List<Long> findUserExamIdsByExamItemId(@Param("itemId") Long itemId);
    
    /**
     * 사용자 시험의 모든 문제 삭제
     */
    void deleteByUserExamId(Long userExamId);
}