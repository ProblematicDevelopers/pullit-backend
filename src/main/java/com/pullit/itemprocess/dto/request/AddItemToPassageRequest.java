package com.pullit.itemprocess.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddItemToPassageRequest {
    private Long passageId;
    private List<Long> itemIds; // 지문에 추가할 문항 ID들
}
