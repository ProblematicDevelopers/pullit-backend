package com.pullit.item.service;

import com.pullit.item.dto.SubjectDTO;
import com.pullit.item.entity.Subject;

import java.util.List;

public interface SubjectService {
    List<Subject> findByAll();
    List<SubjectDTO> findAllSubjectsOnly();
}
