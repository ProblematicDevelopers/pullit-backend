package com.pullit.teacher.repository;

import com.pullit.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    // 학교 ID로 교사 목록 조회
    List<Teacher> findBySchoolId(Long schoolId);

    // 지역 코드로 교사 목록 조회
    @Query("SELECT t FROM Teacher t WHERE t.area.code = :areaCode")
    List<Teacher> findByAreaCode(@Param("areaCode") String areaCode);

    // 지역 이름으로 교사 목록 조회
    @Query("SELECT t FROM Teacher t WHERE t.area.name = :areaName")
    List<Teacher> findByAreaName(@Param("areaName") String areaName);

    // User ID로 Teacher 조회
    Optional<Teacher> findByUserId(Long userId);

    // User 정보와 함께 Teacher 조회
    @Query("SELECT t FROM Teacher t JOIN FETCH t.user WHERE t.userId = :userId")
    Optional<Teacher> findByUserIdWithUser(@Param("userId") Long userId);
    
    // 존재 여부 확인
    boolean existsByUserId(Long userId);
}
