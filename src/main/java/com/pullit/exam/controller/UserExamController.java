package com.pullit.exam.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="UserExam", description = "사용자 시험지 관리 API")
@RestController
@RequestMapping("/api/user-exams")
@RequiredArgsConstructor
@Slf4j
public class UserExamController {

}
