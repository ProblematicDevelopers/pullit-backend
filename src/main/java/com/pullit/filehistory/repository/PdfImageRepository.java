package com.pullit.filehistory.repository;

import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.entity.PdfImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PdfImageRepository extends JpaRepository<PdfImage, Long> {

    /**
     * FileHistory로 PdfImage 목록 조회 (페이지 순서로 정렬)
     */
    List<PdfImage> findByFileHistoryOrderByPageNumber(FileHistory fileHistory);

    /**
     * FileHistory ID로 PdfImage 목록 조회 (페이지 순서로 정렬)
     */
    @Query("SELECT pi FROM PdfImage pi WHERE pi.fileHistory.id = :fileHistoryId ORDER BY pi.pageNumber")
    List<PdfImage> findByFileHistoryIdOrderByPageNumber(@Param("fileHistoryId") Long fileHistoryId);

    /**
     * FileHistory와 페이지 번호로 PdfImage 조회
     */
    Optional<PdfImage> findByFileHistoryAndPageNumber(FileHistory fileHistory, Integer pageNumber);

    /**
     * FileHistory ID로 총 페이지 수 조회
     */
    @Query("SELECT COUNT(pi) FROM PdfImage pi WHERE pi.fileHistory.id = :fileHistoryId")
    long countByFileHistoryId(@Param("fileHistoryId") Long fileHistoryId);

    /**
     * FileHistory로 모든 PdfImage 삭제
     */
    void deleteByFileHistory(FileHistory fileHistory);

    /**
     * S3 키로 PdfImage 조회
     */
    Optional<PdfImage> findByS3Key(String s3Key);
}