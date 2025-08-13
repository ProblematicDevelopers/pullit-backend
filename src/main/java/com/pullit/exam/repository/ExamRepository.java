package com.pullit.exam.repository;

import com.pullit.exam.entity.Exam;
import com.pullit.exam.enums.ExamVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long>, JpaSpecificationExecutor<Exam> {

    @Query("SELECT DISTINCT e from Exam e " +
            "LEFT JOIN FETCH e.subject s " +
            "LEFT JOIN FETCH e.examItems ei " +
            "WHERE e.id = :id"
    )
    Optional<Exam> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT e FROM Exam e " +
            "LEFT JOIN FETCH e.subject s " +
            "WHERE e.visibility = :visibility")
    Page<Exam> findAllByVisibility(@Param("visibility") ExamVisibility visibility, Pageable pageable);

    @Query("SELECT DISTINCT e.examName From Exam e " +
            "WHERE LOWER(e.examName) LIKE LOWER(CONCAT('%',:query,'%')) " +
            "ORDER BY e.updatedDate DESC ")
    List<String> findExamNamesByQuery(@Param("query") String query, Pageable pageable);
}
