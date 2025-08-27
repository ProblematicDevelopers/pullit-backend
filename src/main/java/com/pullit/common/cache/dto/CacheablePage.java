package com.pullit.common.cache.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Redis에서 Page 객체를 캐싱하기 위한 래퍼 클래스
 * PageImpl은 기본 생성자가 없어 직렬화/역직렬화 문제가 발생하므로
 * 이 클래스를 사용하여 Page 데이터를 캐싱합니다.
 */
@Getter
public class CacheablePage<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;

    /**
     * Page 객체로부터 CacheablePage 생성
     */
    public CacheablePage(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }

    /**
     * JSON 역직렬화를 위한 생성자
     * Jackson이 이 생성자를 사용하여 객체를 재구성합니다.
     */
    @JsonCreator
    public CacheablePage(
            @JsonProperty("content") List<T> content,
            @JsonProperty("pageNumber") int pageNumber,
            @JsonProperty("pageSize") int pageSize,
            @JsonProperty("totalElements") long totalElements,
            @JsonProperty("totalPages") int totalPages) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    /**
     * CacheablePage를 Spring Data Page로 변환
     */
    public Page<T> toPage() {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return new PageImpl<>(content, pageable, totalElements);
    }

    /**
     * Page 객체를 캐싱 가능한 형태로 변환하는 정적 팩토리 메서드
     */
    public static <T> CacheablePage<T> from(Page<T> page) {
        return new CacheablePage<>(page);
    }
}