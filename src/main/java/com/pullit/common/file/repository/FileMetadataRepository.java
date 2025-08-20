package com.pullit.common.file.repository;

import com.pullit.common.file.entity.FileMetadata;
import com.pullit.common.file.enums.FileCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    // S3 키로 조회
    Optional<FileMetadata> findByS3Key(String s3Key);
    
    // S3 키로 삭제되지 않은 파일 조회
    Optional<FileMetadata> findByS3KeyAndIsDeletedFalse(String s3Key);
    
    // 엔티티별 파일 조회
    List<FileMetadata> findByEntityTypeAndEntityIdAndIsDeletedFalse(
        String entityType, 
        Long entityId
    );
    
    // 사용자별 파일 조회
    List<FileMetadata> findByUploadedByAndIsDeletedFalse(Long userId);
    
    // 사용자별 파일 페이징 조회
    Page<FileMetadata> findByUploadedByAndIsDeletedFalse(
        Long userId, 
        Pageable pageable
    );
    
    // 만료된 파일 조회
    List<FileMetadata> findByExpiresAtBeforeAndIsDeletedTrue(LocalDateTime dateTime);
    
    // 카테고리별 파일 조회
    @Query("SELECT f FROM FileMetadata f WHERE f.fileCategory = :category " +
           "AND f.isDeleted = false ORDER BY f.createdDate DESC")
    Page<FileMetadata> findByCategory(
        @Param("category") FileCategory category, 
        Pageable pageable
    );
    
    // 여러 ID로 조회
    List<FileMetadata> findByIdInAndIsDeletedFalse(List<Long> ids);
    
    // 소프트 삭제
    @Modifying
    @Query("UPDATE FileMetadata f SET f.isDeleted = true, f.deletedAt = :now " +
           "WHERE f.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);
    
    // 엔티티별 파일 개수 조회
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.entityType = :entityType " +
           "AND f.entityId = :entityId AND f.isDeleted = false")
    long countByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    
    // 기간별 업로드 통계
    @Query("SELECT DATE(f.createdDate) as uploadDate, COUNT(f) as count " +
           "FROM FileMetadata f " +
           "WHERE f.createdDate BETWEEN :startDate AND :endDate " +
           "AND f.isDeleted = false " +
           "GROUP BY DATE(f.createdDate)")
    List<Object[]> getUploadStatistics(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // 용량 통계
    @Query("SELECT SUM(f.fileSize) FROM FileMetadata f WHERE f.uploadedBy = :userId " +
           "AND f.isDeleted = false")
    Long getTotalFileSizeByUser(@Param("userId") Long userId);
    
    // 공개 파일 조회
    Page<FileMetadata> findByIsPublicTrueAndIsDeletedFalse(Pageable pageable);
    
    // 검색 (파일명 기준)
    @Query("SELECT f FROM FileMetadata f WHERE " +
           "(LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND f.isDeleted = false")
    Page<FileMetadata> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}