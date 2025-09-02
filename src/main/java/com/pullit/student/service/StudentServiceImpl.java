package com.pullit.student.service;

import com.pullit.student.dto.response.StudentResponse;
import com.pullit.student.entity.Student;
import com.pullit.student.repository.StudentRepository;
import com.pullit.student.service.StudentService;
import com.pullit.user.dto.request.StudentInfo;
import com.pullit.user.entity.User;
import com.pullit.classes.entity.School;
import com.pullit.classes.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;

    @Override
    public StudentResponse findByUserId(Long userId) {
        Student student = studentRepository.findByUserId(userId);
        return StudentResponse.builder()
                .userId(student.getUserId())
                .classGroupId(student.getClassGroupID())
                .studentNo(student.getStudentNo())
                .grade(student.getGrade())
                .build();
    }

    @Override
    @Transactional
    public Student createStudent(User user, StudentInfo studentInfo) {
        log.info("Creating student for user: {}", user.getUsername());
        
        // Check if student already exists
        if (studentRepository.existsById(user.getId())) {
            log.warn("Student already exists for user ID: {}", user.getId());
            return studentRepository.findByUserId(user.getId());
        }
        
        // 학교 정보 처리
        School school = null;
        if (studentInfo != null && studentInfo.getSchoolId() != null) {
            school = schoolRepository.findById(studentInfo.getSchoolId()).orElse(null);
        } else if (studentInfo != null && studentInfo.getSchoolName() != null) {
            // 학교명으로 학교 찾기 (정확한 이름으로 찾기)
            List<School> schools = schoolRepository.findBySchoolNameContaining(studentInfo.getSchoolName());
            if (!schools.isEmpty()) {
                // 정확한 이름과 일치하는 학교 찾기
                school = schools.stream()
                    .filter(s -> s.getSchoolName().equals(studentInfo.getSchoolName()))
                    .findFirst()
                    .orElse(schools.get(0)); // 정확한 이름이 없으면 첫 번째 학교 사용
            }
        }
        
        Student student = Student.builder()
                .userId(user.getId())
                // user는 설정하지 않음 (insertable=false, updatable=false)
                .classGroupID(studentInfo != null ? studentInfo.getClassGroupId() : null)
                .studentNo(studentInfo != null ? studentInfo.getStudentNo() : null)
                .grade(studentInfo != null ? studentInfo.getGrade() : null)
                .school(school)
                .build();
        
        Student savedStudent = studentRepository.save(student);
        log.info("Student created with ID: {}", savedStudent.getUserId());
        
        return savedStudent;
    }
}