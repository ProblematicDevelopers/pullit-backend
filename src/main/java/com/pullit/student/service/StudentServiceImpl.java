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
@Transactional
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
    public Student createStudent(User user, StudentInfo studentInfo) {
        log.info("Creating student for user: {}", user.getUsername());
        
        Student student = Student.builder()
                .userId(user.getId())
                .user(user)
                .classGroupID(studentInfo != null ? studentInfo.getClassGroupId() : null)
                .studentNo(studentInfo != null ? studentInfo.getStudentNo() : null)
                .grade(studentInfo != null ? studentInfo.getGrade() : null)
                .build();
        
        Student savedStudent = studentRepository.save(student);
        log.info("Student created with ID: {}", savedStudent.getUserId());
        
        return savedStudent;
    }
}