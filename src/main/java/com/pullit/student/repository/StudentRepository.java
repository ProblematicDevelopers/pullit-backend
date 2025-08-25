package com.pullit.student.repository;

import com.pullit.student.entity.Student;
import com.pullit.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    Student findByUserId(Long userId);
    
    // classGroupID로 학생들 조회
    List<Student> findByClassGroupID(Long classGroupID);
    
    // 학생 ID로 User 정보 조회
    @Query("SELECT u FROM User u WHERE u.id = :studentId")
    Optional<User> findUserByStudentId(@Param("studentId") Long studentId);
}
