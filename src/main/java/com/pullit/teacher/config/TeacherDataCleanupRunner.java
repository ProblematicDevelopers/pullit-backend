package com.pullit.teacher.config;

import com.pullit.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Startup runner that cleans up orphaned teacher records on application start
 * This helps prevent OptimisticLockingFailureException issues
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherDataCleanupRunner implements ApplicationRunner {

    private final TeacherService teacherService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Running teacher data cleanup on startup...");
            teacherService.cleanupOrphanedTeachers();
            log.info("Teacher data cleanup completed successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup teacher data on startup", e);
            // Don't fail the application startup if cleanup fails
        }
    }
}