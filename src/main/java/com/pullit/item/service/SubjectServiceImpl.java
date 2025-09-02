package com.pullit.item.service;

import com.pullit.item.dao.SubjectRepository;
import com.pullit.common.annotation.RedisCacheable;
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
    @RedisCacheable(
        key = "'subject:all'",
        ttl = 240,  // 4시간 TTL (과목 정보는 매우 정적)
        timeUnit = java.util.concurrent.TimeUnit.MINUTES
    )
    public List<SubjectResponse> findAllSubjectsOnly() {
        // 1) 기본 과목 정보 조회 (N+1 방지를 위해 itemCount는 여기서 채우지 않음)
        List<Subject> subjects = subjectRepository.findAll();
        List<SubjectResponse> responses = subjects.stream()
                .map(SubjectResponse::from)
                .toList();

        // 2) 과목별 문항 수를 단일 집계 쿼리로 조회 후 매핑
        var counts = subjectRepository.countItemsAll();
        java.util.Map<Long, Integer> countMap = new java.util.HashMap<>();
        for (var p : counts) {
            countMap.put(p.getSubjectId(), p.getItemCount() != null ? p.getItemCount().intValue() : 0);
        }

        responses.forEach(r -> r.setItemCount(countMap.getOrDefault(r.getSubjectId(), 0)));
        return responses;
    }

    @Override
    @RedisCacheable(
        key = "'subject:byGradeArea:' + #gradeCode + ':' + #areaCode",
        ttl = 240,  // 4시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#gradeCode != null && #areaCode != null"
    )
    public List<SubjectResponse> findByGradeCodeAndAreaCode(String gradeCode, String areaCode) {
        // 1) 필터 조건으로 과목 조회
        List<Subject> subjects = subjectRepository.findByGradeCodeAndAreaCode(gradeCode, areaCode);
        List<SubjectResponse> responses = subjects.stream()
                .map(SubjectResponse::from)
                .toList();

        // 2) 같은 필터로 문항 수 집계 후 매핑
        var counts = subjectRepository.countItemsByGradeAndArea(gradeCode, areaCode);
        java.util.Map<Long, Integer> countMap = new java.util.HashMap<>();
        for (var p : counts) {
            countMap.put(p.getSubjectId(), p.getItemCount() != null ? p.getItemCount().intValue() : 0);
        }

        responses.forEach(r -> r.setItemCount(countMap.getOrDefault(r.getSubjectId(), 0)));
        return responses;
    }

    @Override
    @RedisCacheable(
        key = "'subject:detail:' + #id",
        ttl = 240,  // 4시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#id != null"
    )
    public SubjectResponse findById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("과목을 찾을 수 없습니다. id=" + id));
        return SubjectResponse.from(subject);
    }
}
