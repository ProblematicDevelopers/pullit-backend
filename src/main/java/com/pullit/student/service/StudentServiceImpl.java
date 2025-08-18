package com.pullit.student.service;

import com.pullit.student.dto.response.StudentResponse;
import com.pullit.student.entity.Student;
import com.pullit.student.repository.StudentRepository;
import com.pullit.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
}