package com.pullit.pdf.repository;

import com.pullit.pdf.entity.PdfTemplate;
import com.pullit.pdf.entity.TemplateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PdfTemplateRepository extends JpaRepository<PdfTemplate, Long> {

    /**
     * 활성화된 템플릿 중 이름으로 검색
     *
     * @param name 템플릿 이름
     * @return 찾은 템플릿 (Optional로 래핑)
     */
    Optional<PdfTemplate> findByNameAndIsActiveTrue(String name);

    /**
     * 템플릿 타입별로 활성화된 목록 조회
     * @param type 템플릿 타입 (EXAM, ANSWER_SHEET, COMBINED, CUSTOM)
     * @return 해당 타입의 활성화된 템플릿 목록
     */
    List<PdfTemplate> findByTypeAndIsActiveTrueOrderByUsageCountDesc(TemplateType type);

    /**
     *  기본 템플릿 조회
     * @return 기본 템플릿 조회
     */
    List<PdfTemplate> findByIsDefaultTrueAndIsActiveTrueOrderByName();

    /**
     * 특정 사용자가 생성한 커스텀 템플릿 목록 조회
     * @param createdBy 생성자 ID
     * @param pageable 페이징 정보
     * @return 사용자의 커스텀 템플릿 페이지
     */
    Page<PdfTemplate> findByCreatedByAndIsActiveTrue(Long createdBy, Pageable pageable);

    /**
     * 키워드로 템플릿 검색 (이름 또는 설명에 포함)
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 페이지
     */
    @Query("SELECT t FROM PdfTemplate t WHERE "+
    "t.isActive = true AND " +
    "(LOWER(t.name) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
    "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<PdfTemplate> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 가장 많이 사용된 템플릿 Top N 조회
     *
     * @param limit 조회할 개수
     * @return 인기 템플릿 목록
     */
    @Query("SELECT t FROM PdfTemplate t WHERE t.isActive = true " +
            "ORDER BY t.usageCount DESC")
    List<PdfTemplate> findTopTemplates(Pageable pageable);

    /**
     * 템플릿 사용 횟수 증가
     * 템플릿으로 PDF 생성 시 호출됩니다.
     *
     * @param templateId 템플릿 ID
     */
    @Modifying
    @Query("UPDATE PdfTemplate t SET t.usageCount = t.usageCount + 1 " +
            "WHERE t.id = :templateId")
    void incrementUsageCount(@Param("templateId") Long templateId);

    /**
     * 템플릿 비활성화 (소프트 삭제)
     * 실제로 삭제하지 않고 isActive를 false로 변경합니다.
     *
     * @param templateId 템플릿 ID
     */
    @Modifying
    @Query("UPDATE PdfTemplate t SET t.isActive = false " +
            "WHERE t.id = :templateId")
    void softDelete(@Param("templateId") Long templateId);

    /**
     * 특정 타입의 기본 템플릿 조회
     * 각 타입별로 하나의 기본 템플릿이 있을 수 있습니다.
     *
     * @param type 템플릿 타입
     * @return 기본 템플릿
     */
    Optional<PdfTemplate> findFirstByTypeAndIsDefaultTrueAndIsActiveTrue(TemplateType type);

    /**
     * 템플릿 이름 중복 확인
     * 새 템플릿 생성 시 이름 중복을 체크합니다.
     *
     * @param name 확인할 템플릿 이름
     * @return 존재 여부
     */
    boolean existsByNameAndIsActiveTrue(String name);

    /**
     * 특정 사용자의 템플릿 개수 조회
     * 사용자별 템플릿 생성 제한을 확인할 때 사용합니다.
     *
     * @param createdBy 사용자 ID
     * @return 템플릿 개수
     */
    @Query("SELECT COUNT(t) FROM PdfTemplate t " +
            "WHERE t.createdBy = :createdBy AND t.isActive = true")
    long countByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * 템플릿 버전 업데이트
     * 템플릿 수정 시 버전을 증가시킵니다.
     *
     * @param templateId 템플릿 ID
     */
    @Modifying
    @Query("UPDATE PdfTemplate t SET t.version = t.version + 1, " +
            "t.modifiedDate = CURRENT_TIMESTAMP " +
            "WHERE t.id = :templateId")
    void incrementVersion(@Param("templateId") Long templateId);

    /**
     * 페이지 크기별 템플릿 조회
     *
     * @param pageSize 페이지 크기 (A4, A3, LETTER 등)
     * @return 해당 페이지 크기의 템플릿 목록
     */
    List<PdfTemplate> findByPageSizeAndIsActiveTrueOrderByName(String pageSize);

    /**
     * 복합 조건 검색을 위한 동적 쿼리
     * QueryDSL이나 Specification을 사용하면 더 유연하게 구현 가능합니다.
     *
     * @param type 템플릿 타입 (nullable)
     * @param pageSize 페이지 크기 (nullable)
     * @param isDefault 기본 템플릿 여부 (nullable)
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @Query("SELECT t FROM PdfTemplate t WHERE " +
            "t.isActive = true AND " +
            "(:type IS NULL OR t.type = :type) AND " +
            "(:pageSize IS NULL OR t.pageSize = :pageSize) AND " +
            "(:isDefault IS NULL OR t.isDefault = :isDefault)")
    Page<PdfTemplate> findByFilters(@Param("type") TemplateType type,
                                    @Param("pageSize") String pageSize,
                                    @Param("isDefault") Boolean isDefault,
                                    Pageable pageable);

}
