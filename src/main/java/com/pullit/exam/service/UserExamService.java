package com.pullit.exam.service;

import com.pullit.exam.dto.request.UserExamCreateRequest;
import com.pullit.exam.dto.response.UserExamResponse;

public interface UserExamService {
    UserExamResponse createExam(UserExamCreateRequest request, String pdfUrl, String answerPdfUrl);
    UserExamResponse updatePdfUrl(Long examId, String pdfUrl);
    UserExamResponse getExam(Long examId);
    void deleteExam(Long examId);
}
