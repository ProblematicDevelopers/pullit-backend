package com.pullit.classes.service;

import com.pullit.classes.entity.School;
import com.pullit.classes.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchoolServiceImpl  implements SchoolService {

    private final SchoolRepository schoolRepository;

    @Override
    public List<School> findBySchoolNameContaining(String schoolName) {
        if(schoolRepository.findBySchoolNameContaining(schoolName).isEmpty()) {
            log.info("검색된 학교가 없습니다.");
            return new ArrayList<>();
        }
        return schoolRepository.findBySchoolNameContaining(schoolName);
    }
    
    @Override
    public List<School> findAll() {
        return schoolRepository.findAll();
    }
    
    @Override
    public School findById(Long schoolId) {
        return schoolRepository.findById(schoolId).orElse(null);
    }
}
