package com.pullit.cbt.repository;

import com.pullit.cbt.entity.AttemptExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<AttemptExam, Long> {

    @Query("""
        SELECT ae
        FROM AttemptExam ae
        JOIN ae.exam e
        JOIN ae.user u
        WHERE e.areaCode = :areaCode
          AND u.id = :userId
    """)
    List<AttemptExam> findByAreaCodeAndUserId(
            @Param("areaCode") String areaCode,
            @Param("userId") Long userId
    );

}
