package com.pullit.cbt.repository;

import com.pullit.cbt.entity.AttemptExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptExamQuestionRepository extends JpaRepository<AttemptExamQuestion, Long> {
    List<AttemptExamQuestion> findByAttemptExamId(Long attemptExamId);
}
