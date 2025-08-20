package com.pullit.cbt.repository;

import com.pullit.cbt.entity.AttemptExam;
import com.pullit.exam.entity.UserExamItem;
import com.pullit.item.entity.ItemMetadata;
import com.pullit.item.entity.ItemHtmlData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<AttemptExam, Long> {

    @Query("""
        SELECT ae
        FROM AttemptExam ae
        JOIN FETCH ae.exam e
        JOIN FETCH ae.user u
        LEFT JOIN FETCH ae.attemptQuestions q
        LEFT JOIN FETCH q.examItem i
        WHERE e.areaCode = :areaCode
          AND u.id = :userId
    """)
    List<AttemptExam> findByAreaCodeAndUserId(
            @Param("areaCode") String areaCode,
            @Param("userId") Long userId
    );

    @Query("""
        SELECT ae
        FROM AttemptExam ae
        JOIN FETCH ae.exam e
        JOIN FETCH ae.user u
        LEFT JOIN FETCH ae.attemptQuestions q
        LEFT JOIN FETCH q.examItem i
        WHERE ae.id = :attemptId
          AND u.id = :userId
    """)
    Optional<AttemptExam> findByAttemptIdAndUserId(
            @Param("attemptId") Long attemptId,
            @Param("userId") Long userId
    );

    // ===== CBT 문항 조회 관련 메서드 =====

    /**
     * question_id로 UserExamItem 조회
     */
    @Query("SELECT uei FROM UserExamItem uei WHERE uei.id = :questionId")
    UserExamItem findUserExamItemById(@Param("questionId") Long questionId);

    /**
     * item_id로 ItemMetadata와 HTML 데이터를 함께 조회
     */
    @Query("SELECT i FROM ItemMetadata i " +
            "LEFT JOIN FETCH i.htmlData " +
            "WHERE i.itemId = :itemId")
    Optional<ItemMetadata> findItemMetadataWithHtmlData(@Param("itemId") Long itemId);
}

