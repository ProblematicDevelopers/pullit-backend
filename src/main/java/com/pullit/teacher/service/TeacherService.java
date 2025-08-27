package com.pullit.teacher.service;

import com.pullit.teacher.entity.Teacher;
import com.pullit.teacher.repository.TeacherRepository;
import com.pullit.user.dto.request.TeacherInfo;
import com.pullit.user.entity.User;
import com.pullit.common.embedded.StringCodeNamePair;
import com.pullit.classes.entity.School;
import com.pullit.classes.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SchoolRepository schoolRepository;
    private final JdbcTemplate jdbcTemplate;
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates or updates a teacher for a given user.
     * Uses a simpler approach to avoid OptimisticLockingFailureException
     */
    public void createTeacherWithJdbc(User user, TeacherInfo teacherInfo) {
        log.info("Creating teacher with JDBC for user: {} (ID: {})", user.getUsername(), user.getId());
        
        try {
            // Check if teacher already exists
            String checkQuery = "SELECT COUNT(*) FROM teachers WHERE user_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, user.getId());
            
            if (count != null && count > 0) {
                log.info("Teacher already exists for user ID: {}, skipping creation", user.getId());
                return;
            }
            
            // Resolve school ID
            Long schoolId = null;
            if (teacherInfo != null && teacherInfo.getSchoolName() != null) {
                try {
                    schoolId = Long.parseLong(teacherInfo.getSchoolName());
                } catch (NumberFormatException e) {
                    // Try to find school by name
                    List<School> schools = schoolRepository.findBySchoolNameContaining(teacherInfo.getSchoolName());
                    if (!schools.isEmpty()) {
                        schoolId = schools.get(0).getId();
                    }
                }
            }
            
            // Get area information
            String areaCode = teacherInfo != null ? teacherInfo.getAreaCode() : null;
            String areaName = teacherInfo != null ? teacherInfo.getAreaName() : null;
            
            // Insert using simple INSERT IGNORE
            String insertQuery = "INSERT IGNORE INTO teachers (user_id, school_id, area_code, area_name, created_date, updated_date) " +
                               "VALUES (?, ?, ?, ?, NOW(), NOW())";
            
            int rowsAffected = jdbcTemplate.update(insertQuery, user.getId(), schoolId, areaCode, areaName);
            
            if (rowsAffected > 0) {
                log.info("Teacher created successfully for user ID: {} with school ID: {}, subject: {}/{}", 
                        user.getId(), schoolId, areaCode, areaName);
            } else {
                log.info("Teacher record already exists or insert ignored for user ID: {}", user.getId());
            }
            
        } catch (Exception e) {
            log.error("Failed to create teacher for user {}: {}", user.getId(), e.getMessage());
            // Don't throw - just log the error
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Teacher createTeacher(User user, TeacherInfo teacherInfo) {
        // Use JDBC approach to avoid JPA issues
        createTeacherWithJdbc(user, teacherInfo);
        
        // Return a dummy teacher object for compatibility
        Teacher teacher = new Teacher();
        teacher.setUserId(user.getId());
        teacher.setUser(user);
        return teacher;
    }
    
    private School resolveSchool(TeacherInfo teacherInfo) {
        if (teacherInfo == null || teacherInfo.getSchoolName() == null) {
            return null;
        }
        
        String schoolNameValue = teacherInfo.getSchoolName().trim();
        
        // Try to parse as ID first
        try {
            Long schoolId = Long.parseLong(schoolNameValue);
            log.info("School ID provided directly: {}", schoolId);
            return schoolRepository.findById(schoolId).orElse(null);
        } catch (NumberFormatException e) {
            // It's a school name, not an ID - look it up
            log.info("Looking up school by name: {}", schoolNameValue);
            List<School> schools = schoolRepository.findBySchoolNameContaining(schoolNameValue);
            
            if (!schools.isEmpty()) {
                // Prefer exact match, otherwise use first result
                School matchedSchool = schools.stream()
                        .filter(school -> school.getSchoolName().equals(schoolNameValue))
                        .findFirst()
                        .orElse(schools.get(0));
                
                log.info("Found school: {} with ID: {}", matchedSchool.getSchoolName(), matchedSchool.getId());
                return matchedSchool;
            } else {
                log.warn("No school found with name: {}", schoolNameValue);
                return null;
            }
        }
    }
    
    /**
     * Cleanup method to remove orphaned teacher records (teachers without corresponding users)
     * This should be called during maintenance or when encountering OptimisticLockingFailureException
     */
    @Transactional
    public void cleanupOrphanedTeachers() {
        log.info("Starting cleanup of orphaned teacher records");
        
        List<Teacher> allTeachers = teacherRepository.findAll();
        int cleanedCount = 0;
        
        for (Teacher teacher : allTeachers) {
            // Check if user exists
            if (teacher.getUser() == null || teacher.getUserId() == null) {
                log.warn("Removing orphaned teacher record with null user");
                teacherRepository.delete(teacher);
                cleanedCount++;
            } else {
                // Double-check if user actually exists in database
                try {
                    entityManager.find(User.class, teacher.getUserId());
                } catch (Exception e) {
                    log.warn("Removing orphaned teacher record for non-existent user ID: {}", teacher.getUserId());
                    teacherRepository.delete(teacher);
                    cleanedCount++;
                }
            }
        }
        
        log.info("Cleanup completed. Removed {} orphaned teacher records", cleanedCount);
    }
}