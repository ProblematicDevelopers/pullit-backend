package com.pullit.student.service;

import com.pullit.student.dto.request.StudentUpdateRequest;
import com.pullit.student.dto.response.StudentResponse;
import com.pullit.student.entity.Student;
import com.pullit.user.dto.request.StudentInfo;
import com.pullit.user.entity.User;

public interface StudentService {
    StudentResponse findByUserId(Long userId);
    Student createStudent(User user, StudentInfo studentInfo);
    StudentResponse updateStudent(Long userId, StudentUpdateRequest request);
}
