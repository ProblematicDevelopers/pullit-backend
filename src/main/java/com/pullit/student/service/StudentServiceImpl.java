package com.pullit.student.service;

import com.pullit.student.dto.response.StudentResponse;
import com.pullit.student.entity.Student;
import com.pullit.student.repository.StudentRepository;
import com.pullit.student.service.StudentService;
import com.pullit.user.dto.request.StudentInfo;
import com.pullit.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

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
        
        Student student = Student.builder()
                .userId(user.getId())
                // user는 설정하지 않음 (insertable=false, updatable=false)
                .classGroupID(studentInfo != null ? studentInfo.getClassGroupId() : null)
                .studentNo(studentInfo != null ? studentInfo.getStudentNo() : null)
                .grade(studentInfo != null ? studentInfo.getGrade() : null)
                .build();
        
        Student savedStudent = studentRepository.save(student);
        log.info("Student created with ID: {}", savedStudent.getUserId());
        
        return savedStudent;
    }
}