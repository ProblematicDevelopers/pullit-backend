package com.pullit.classes.service;

import com.pullit.classes.entity.School;
import com.pullit.classes.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchoolServiceImpl  implements SchoolService {

    private final SchoolRepository schoolRepository;

    @Override
    public List<School> findBySchoolNameContaining(String schoolName) {
        return schoolRepository.findBySchoolNameContaining(schoolName);
    }
}
