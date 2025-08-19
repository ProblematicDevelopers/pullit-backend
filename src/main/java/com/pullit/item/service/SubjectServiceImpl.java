package com.pullit.item.service;

import com.pullit.item.dao.SubjectRepository;
import com.pullit.item.dto.response.SubjectResponse;
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
    public List<SubjectResponse> findAllSubjectsOnly() {
        return subjectRepository.findAll().stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Override
    public List<SubjectResponse> findByGradeCodeAndAreaCode(String gradeCode, String areaCode) {
        return subjectRepository.findByGradeCodeAndAreaCode(gradeCode, areaCode).stream().map(SubjectResponse::from).toList();
    }

    @Override
    public SubjectResponse findById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("과목을 찾을 수 없습니다. id=" + id));
        return SubjectResponse.from(subject);
    }
}
