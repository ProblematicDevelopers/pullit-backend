package com.pullit.item.service;

import com.pullit.item.dao.SubjectRepository;
import com.pullit.item.dto.SubjectDTO;
import com.pullit.item.entity.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    @Override
    public List<Subject> findByAll() {
        return subjectRepository.findAll();
    }

    @Override
    public List<SubjectDTO> findAllSubjectsOnly() {
        return subjectRepository.findAllSubjectsOnly().stream()
                .map(SubjectDTO::from)
                .toList();
    }
}
