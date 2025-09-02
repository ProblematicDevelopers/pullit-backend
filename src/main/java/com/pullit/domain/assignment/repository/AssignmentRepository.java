package com.pullit.domain.assignment.repository;

import com.pullit.domain.assignment.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    // 선생님이 생성한 과제 목록 조회
    Page<Assignment> findByTeacherId(Long teacherId, Pageable pageable);
    
    // 특정 반에 할당된 과제 목록 조회
    @Query("SELECT DISTINCT a FROM Assignment a " +
           "JOIN a.assignmentClasses ac " +
           "WHERE ac.classId = :classId " +
           "AND a.status = :status")
    Page<Assignment> findByClassIdAndStatus(@Param("classId") Long classId, 
                                           @Param("status") Assignment.AssignmentStatus status, 
                                           Pageable pageable);
    
    // 학생이 속한 반들의 과제 목록 조회
    @Query("SELECT DISTINCT a FROM Assignment a " +
           "JOIN a.assignmentClasses ac " +
           "WHERE ac.classId IN :classIds " +
           "AND a.status IN :statuses " +
           "ORDER BY a.dueDate ASC")
    List<Assignment> findByClassIdsAndStatuses(@Param("classIds") List<Long> classIds,
                                               @Param("statuses") List<Assignment.AssignmentStatus> statuses);
    
    // 마감 임박 과제 조회
    @Query("SELECT a FROM Assignment a " +
           "WHERE a.dueDate BETWEEN :now AND :deadline " +
           "AND a.status = 'PUBLISHED'")
    List<Assignment> findUpcomingDeadlines(@Param("now") LocalDateTime now,
                                          @Param("deadline") LocalDateTime deadline);
    
    // 과제 상세 조회 (연관 엔티티 포함)
    @Query("SELECT a FROM Assignment a " +
           "LEFT JOIN FETCH a.files " +
           "LEFT JOIN FETCH a.assignmentClasses " +
           "WHERE a.id = :id")
    Optional<Assignment> findByIdWithDetails(@Param("id") Long id);
}