package com.pullit.item.elastic.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import com.pullit.item.elastic.document.ItemImageDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemImageService {

    private static final String INDEX_NAME = "item_image";

    private final ElasticsearchClient elasticsearchClient;

    public ItemImageService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<ItemImageDocument> findSimilarItems(
            long topicChapterId,
            int difficultyCode,
            List<Long> excludeItemIds,
            int size
    ) throws IOException {

        // 상위 단계 코드 추출
        long subjectId = truncateCode(topicChapterId, 4);
        long largeChapterId = truncateCode(topicChapterId, 6);
        long mediumChapterId = truncateCode(topicChapterId, 8);
        long smallChapterId = truncateCode(topicChapterId, 10);

        // 기본 필터: 난이도 동일, 제외 문항 제외
        BoolQuery.Builder baseBoolQuery = new BoolQuery.Builder()
                .must(m -> m.term(t -> t.field("difficultyCode").value(difficultyCode)));

        if (excludeItemIds != null && !excludeItemIds.isEmpty()) {
            baseBoolQuery.mustNot(mn -> mn.terms(t -> t
                    .field("itemId")
                    .terms(tt -> tt.value(
                            excludeItemIds.stream().map(FieldValue::of).collect(Collectors.toList())
                    ))
            ));
        }

        // function_score 쿼리: 계층별 필터 + 가중치
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q.functionScore(fs -> fs
                        .query(qb -> qb.bool(baseBoolQuery.build()))
                        .functions(
                                buildWeightedFilter("subjectId", subjectId, 1.0),
                                buildWeightedFilter("largeChapterId", largeChapterId, 2.0),
                                buildWeightedFilter("mediumChapterId", mediumChapterId, 3.0),
                                buildWeightedFilter("smallChapterId", smallChapterId, 4.0),
                                buildWeightedFilter("topicChapterId", topicChapterId, 5.0)
                        )
                        .scoreMode(FunctionScoreMode.Sum)
                        .boostMode(FunctionBoostMode.Replace)
                ))
                .size(size)
        );

        SearchResponse<ItemImageDocument> response = elasticsearchClient.search(searchRequest, ItemImageDocument.class);

        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    // 상위 단계 코드 자르기
    private long truncateCode(long code, int length) {
        String codeStr = String.valueOf(code);
        if (codeStr.length() < length) {
            return code;
        }
        return Long.parseLong(codeStr.substring(0, length));
    }

    // Weighted Filter 생성
    private FunctionScore buildWeightedFilter(String fieldName, long value, double weight) {
        return FunctionScore.of(f -> f
                .filter(q -> q.term(t -> t.field(fieldName).value(value)))
                .weight(weight)
        );
    }
}