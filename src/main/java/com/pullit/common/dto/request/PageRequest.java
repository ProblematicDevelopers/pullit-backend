package com.pullit.common.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private Integer page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private Integer size = 20;

    private String sortBy = "createdAt";

    private String sortDirection = "DESC";

    // Spring Data Pageable 객체로 변환
    public org.springframework.data.domain.PageRequest toPageable() {
        Sort.Direction direction = Sort.Direction.fromString(this.sortDirection);
        return org.springframework.data.domain.PageRequest.of(
                this.page,
                this.size,
                Sort.by(direction, this.sortBy)
        );
    }

    // 정렬 조건이 여러 개인 경우
    public org.springframework.data.domain.PageRequest toPageable(Sort sort) {
        return org.springframework.data.domain.PageRequest.of(
                this.page,
                this.size,
                sort
        );
    }

}
