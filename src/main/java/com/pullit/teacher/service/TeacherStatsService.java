package com.pullit.teacher.service;

import com.pullit.teacher.dto.response.*;
import java.util.List;

public interface TeacherStatsService {
    
    // 클래스 전체 성적 개요
    ClassGradeOverviewResponse getClassGradeOverview(Long classId, Long teacherId);
    
    // 전체 학생 성적 목록
    List<StudentGradeResponse> getAllStudentsGrades(Long classId, Long examId, Long teacherId);
    
    // 특정 시험 결과 상세
    ExamResultDetailResponse getExamResultDetail(Long classId, Long examId, Long teacherId);
    
    // 개별 학생 성적 상세
    StudentDetailGradeResponse getStudentGradeDetail(Long classId, Long studentId, Long teacherId);
    
    // 성적 분포도
    GradeDistributionResponse getGradeDistribution(Long classId, Long examId, Long teacherId);
    
    // 시험별 비교
    List<ExamComparisonResponse> getExamComparison(Long classId, Long teacherId);
    
    // 교사 권한 확인
    boolean isTeacherOfClass(Long teacherId, Long classId);
}