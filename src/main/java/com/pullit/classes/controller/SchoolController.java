package com.pullit.classes.controller;

import com.pullit.exam.service.ExamSearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools")
@Slf4j
@Tag(name = "School", description = "학교 관련 API")
@RequiredArgsConstructor
public class SchoolController {

    private final ExamSearchService examSearchService;


}
