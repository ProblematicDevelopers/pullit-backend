package com.pullit.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;          // 실제 데이터
    private int pageNumber;           // 현재 페이지 번호
    private int pageSize;             // 페이지 크기
    private long totalElements;       // 전체 데이터 개수
    private int totalPages;           // 전체 페이지 수
    private boolean first;            // 첫 페이지 여부
    private boolean last;             // 마지막 페이지 여부
    private boolean empty;            // 데이터 없음 여부
    private int numberOfElements;     // 현재 페이지의 데이터 개수

    // Spring Data Page 객체로부터 생성
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    // 데이터 변환 후 생성
    public static <T, R> PageResponse<R> from(Page<T> page, List<R> content) {
        return PageResponse.<R>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .numberOfElements(content.size())
                .build();
    }
}
