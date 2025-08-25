package com.pullit.classes.service;

import com.pullit.classes.dto.response.ClassDetailResponse;
import com.pullit.classes.dto.response.StudentInfoResponse;
import com.pullit.classes.dto.response.TeacherInfoResponse;
import com.pullit.classes.entity.Classes;
import com.pullit.classes.repository.ClassRepository;
import com.pullit.student.entity.Student;
import com.pullit.student.repository.StudentRepository;
import com.pullit.teacher.entity.Teacher;
import com.pullit.teacher.repository.TeacherRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
@Tag(name = "Classes", description = "클래스 API")
public class ClassesServiceImpl implements ClassesService {

    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Operation(summary = "클래스 ID로 특정 클래스의 상세 정보를 조회", description = "클래스 ID로 특정 클래스의 상세 정보를 조회")
    public ClassDetailResponse getClassDetailById(Long userId) {
        log.info("Getting class detail for class ID: {}", userId);
        Student userStudent = studentRepository.findByUserId(userId);
        Long classId = userStudent.getClassGroupID();
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("클래스를 찾을 수 없습니다. ID: " + classId));
        
        return convertToClassDetailResponse(classEntity);
    }

    private ClassDetailResponse convertToClassDetailResponse(Classes classEntity) {
        // 담당 교사 정보 조회
        TeacherInfoResponse teacherInfo = getTeacherInfo(classEntity.getTeacherId());
        
        // 클래스에 속한 학생들 정보 조회 (임시로 빈 리스트 반환)
        List<StudentInfoResponse> students = getStudentsInClass(classEntity.getClassId());
        
        return ClassDetailResponse.builder()
                .classId(classEntity.getClassId())
                .className(classEntity.getClassName())
                .classGrade(classEntity.getClassGrade())
                .classSubject(classEntity.getClassSubject())
                .createdDate(classEntity.getCreatedDate())
                .updatedDate(classEntity.getUpdatedDate())
                .teacher(teacherInfo)
                .students(students)
                .totalStudents((long) students.size())
                .totalTeachers(1L)
                .build();
    }

    private List<StudentInfoResponse> getStudentsInClass(Long classId) {
        log.info("Getting students for class ID: {}", classId);
        
        List<Student> students = studentRepository.findByClassGroupID(classId);
        log.info("Found {} students for class ID: {}", students.size(), classId);
        
        // StudentInfoResponse로 변환
        List<StudentInfoResponse> studentInfoResponses = students.stream()
                .map(student -> {
                    // User 정보 조회
                    var user = studentRepository.findUserByStudentId(student.getUserId())
                            .orElse(null);
                    return StudentInfoResponse.builder()
                            .studentId(student.getUserId())
                            .studentName(user != null ? user.getFullName() : "Unknown")
                            .email(user != null ? user.getEmail() : "")
                            .phoneNumber(user != null ? user.getPhone() : "")
                            .grade(student.getGrade())
                            .studentNumber(student.getStudentNo().toString())
                            .enrolledDate(student.getCreatedDate().toLocalDate())
                            .status("OFFLINE")
                            .build();
                })
                .collect(Collectors.toList());
        
        return studentInfoResponses;
    }

    private TeacherInfoResponse getTeacherInfo(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        
        Teacher teacher = teacherRepository.findByUserIdWithUser(teacherId)
                .orElse(null);
        
        if (teacher == null) {
            log.warn("Teacher not found for ID: {}", teacherId);
            return null;
        }
        
        return TeacherInfoResponse.builder()
                .teacherId(teacher.getUserId())
                .teacherName(teacher.getUser().getFullName())
                .email(teacher.getUser().getEmail())
                .phoneNumber(teacher.getUser().getPhone())
                .subject(teacher.getAreaDisplayName())
                .build();
    }
}
