package com.pullit.classes.repository;

import com.pullit.classes.entity.ClassInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassInvitationRepository extends JpaRepository<ClassInvitation, Long> {
    
    Optional<ClassInvitation> findByInviteCodeAndIsActiveTrue(String inviteCode);
    
    Optional<ClassInvitation> findByClassIdAndIsActiveTrue(Long classId);
    
    @Query("SELECT ci FROM ClassInvitation ci WHERE ci.inviteCode = :code " +
           "AND ci.isActive = true AND ci.expiresAt > CURRENT_TIMESTAMP")
    Optional<ClassInvitation> findActiveByInviteCode(@Param("code") String inviteCode);
    
    boolean existsByInviteCode(String inviteCode);
    
    @Modifying
    @Query("UPDATE ClassInvitation ci SET ci.isActive = false WHERE ci.classId = :classId")
    void deactivateAllByClassId(@Param("classId") Long classId);
}