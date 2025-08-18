package com.pullit.student.service;

import com.pullit.student.dto.response.StudentResponse;

public interface StudentService {
    StudentResponse findByUserId(Long userId);
}
