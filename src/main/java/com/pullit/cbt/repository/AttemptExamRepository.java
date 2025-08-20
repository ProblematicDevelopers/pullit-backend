package com.pullit.cbt.repository;

import com.pullit.cbt.entity.AttemptExam;
import com.pullit.exam.entity.UserExam;
import com.pullit.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttemptExamRepository extends JpaRepository<AttemptExam, Long> {
    AttemptExam findByUserAndExamAndStatus(User user, UserExam exam, AttemptExam.AttemptStatus status);
}
