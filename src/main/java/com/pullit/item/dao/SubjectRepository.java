package com.pullit.item.dao;

import com.pullit.item.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer>{
    @Query("select s from Subject s")
    List<Subject> findAllSubjectsOnly();
}
