package com.pullit.classes.entity;

import com.pullit.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassInvitation extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;
    
    @Column(name = "class_id", nullable = false)
    private Long classId;
    
    @Column(name = "invite_code", unique = true, nullable = false, length = 8)
    private String inviteCode;  // 예: "MATH2A3B"
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;  // 기본 30일
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "max_uses")
    private Integer maxUses;  // null = 무제한
    
    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;
    
    @Column(name = "created_by")
    private Long createdBy;  // 생성한 선생님 ID
    
    // 초대코드 사용 가능 여부 체크
    public boolean isUsable() {
        if (!isActive) return false;
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) return false;
        if (maxUses != null && usedCount >= maxUses) return false;
        return true;
    }
    
    // 사용 횟수 증가
    public void incrementUsage() {
        this.usedCount = (this.usedCount == null ? 0 : this.usedCount) + 1;
    }
}