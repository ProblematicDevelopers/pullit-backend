package com.pullit.classes.service;


import com.pullit.classes.entity.School;

import java.util.List;

public interface SchoolService {
    List<School> findBySchoolNameContaining(String schoolName);
}
