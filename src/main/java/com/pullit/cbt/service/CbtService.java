package com.pullit.cbt.service;

import com.pullit.cbt.dto.request.CbtExamCreateRequest;

public interface CbtService {
    Long createExam(Long userId, CbtExamCreateRequest request);

    void addExamItem(Long examId, CbtExamCreateRequest request);
}
