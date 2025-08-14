package com.pullit.item.service;

import com.pullit.item.dto.response.SubjectResponse;
import com.pullit.item.entity.Subject;

import java.util.List;

public interface SubjectService {
    List<Subject> findByAll();
    List<SubjectResponse> findAllSubjectsOnly();
}
