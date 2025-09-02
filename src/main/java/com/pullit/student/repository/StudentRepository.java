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
    
    // 추가 메서드들
    Optional<Student> findByUserIdAndClassGroupID(Long userId, Long classGroupId);
    
    boolean existsByUserIdAndClassGroupID(Long userId, Long classGroupId);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.classGroupID = :classId")
    Long countByClassId(@Param("classId") Long classId);
    
    @Query("SELECT MAX(s.studentNo) FROM Student s WHERE s.classGroupID = :classId")
    Optional<Long> findMaxStudentNoByClassId(@Param("classId") Long classId);
    
    // 학급 미배정 학생 조회
    List<Student> findByClassGroupIDIsNull();
    
    // 학교별 학급 미배정 학생 조회
    List<Student> findBySchoolIdAndClassGroupIDIsNull(Long schoolId);
    
    // 학교별 학급 미배정 학생 조회 (학년 코드 필터링: 07, 08, 09)
    List<Student> findBySchoolIdAndGrade_CodeAndClassGroupIDIsNull(Long schoolId, String gradeCode);
}
