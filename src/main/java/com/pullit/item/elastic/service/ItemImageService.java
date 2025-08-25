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
import java.util.ArrayList;
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
            long passageId,
            List<Long> excludeItemIds,
            int size
    ) throws IOException {

        // 상위 단계 코드 추출
        long subjectId = truncateCode(topicChapterId, 4);
        long largeChapterId = truncateCode(topicChapterId, 6);
        long mediumChapterId = truncateCode(topicChapterId, 8);
        long smallChapterId = truncateCode(topicChapterId, 10);

        
        // 기본 필터 기능
        BoolQuery.Builder baseBoolQuery = new BoolQuery.Builder();
        
        // 지문 id 동일
        if (passageId != -1) {
            baseBoolQuery.must(mn -> mn.term(t -> t
                    .field("passage_id")
                    .value(passageId)
            ));
        } else {
            baseBoolQuery.mustNot(mn -> mn.exists(e -> e
                    .field("passage_id")
            ));
        }

        // 제외 문항 제외
        if (excludeItemIds != null && !excludeItemIds.isEmpty()) {
            baseBoolQuery.mustNot(mn -> mn.terms(t -> t
                    .field("item_id")
                    .terms(tt -> tt.value(
                            excludeItemIds.stream().map(FieldValue::of).collect(Collectors.toList())
                    ))
            ));
        }

        // 가중치 함수
        List<FunctionScore> functions = new ArrayList<>();

        // difficulty_code 가중치
        if (1 <= difficultyCode && difficultyCode <= 5) {
            int stdDifficultyCode = passageId != -1? 1 : difficultyCode;
            for (int diff = 1; diff <= 5; diff++) {
                double weight = 5.0 - Math.abs(diff - stdDifficultyCode);
                final int diffValue = diff;
                functions.add(FunctionScore.of(f -> f
                        .filter(q -> q.term(t -> t.field("difficulty_code").value(diffValue)))
                        .weight(weight)
                ));
            }
        }

        // 계층형 id 기반 가중치
        functions.add(buildWeightedFilter("subject_id", subjectId, 15.0));
        functions.add(buildWeightedFilter("large_chapter_id", largeChapterId, 25.0));
        functions.add(buildWeightedFilter("medium_chapter_id", mediumChapterId, 35.0));
        functions.add(buildWeightedFilter("small_chapter_id", smallChapterId, 45.0));
        functions.add(buildWeightedFilter("topic_chapter_id", topicChapterId, 55.0));


        // function_score 쿼리: 계층별 필터 + 가중치
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q.functionScore(fs -> fs
                        .query(qb -> qb.bool(baseBoolQuery.build()))
                        .functions(functions)
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