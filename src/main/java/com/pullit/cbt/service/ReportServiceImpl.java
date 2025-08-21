package com.pullit.cbt.service;

import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.cbt.dto.response.DetailDifficultyResponse;
import com.pullit.cbt.dto.response.DetailErrataResponse;
import com.pullit.cbt.dto.response.DetailEvaluationResponse;
import com.pullit.cbt.entity.AttemptExam;
import com.pullit.cbt.projection.DetailDifficultyProjection;
import com.pullit.cbt.repository.ReportRepository;
import com.pullit.item.enums.AreaCode;
import com.pullit.item.dto.response.ItemSearchResponse;
import com.pullit.item.dao.ItemHtmlDataRepository;
import com.pullit.item.entity.ItemMetadata;
import com.pullit.item.entity.ItemHtmlData;
import com.pullit.item.entity.ItemImageData;
import com.pullit.exam.entity.UserExamItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.geom.Area;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ItemHtmlDataRepository itemHtmlDataRepository;

    @Override
    public List<AttemptExamResponse> findAttemptExamBySubjectId(Long userId, AreaCode areaCode) {
        return reportRepository
                .findByAreaCodeAndUserId(AreaCode.getStringCode(areaCode), userId)
                .stream()
                .map(AttemptExam::convertToResponseExclude)
                .collect(Collectors.toList());
    }

    public AttemptExamResponse findAttemptExamDetailById(Long attemptId, Long userId) {
        return reportRepository
                .findByAttemptIdAndUserId(attemptId, userId)
                .map(AttemptExam::convertToResponseWithQuestions)
                .orElseThrow(() -> new RuntimeException("Attempt exam not found"));
    }

    @Override
    public ItemSearchResponse getItemByQuestionId(Long questionId) {
        // 1. question_id로 UserExamItem 조회
        UserExamItem userExamItem = reportRepository.findUserExamItemById(questionId);
        if (userExamItem == null) {
            throw new IllegalArgumentException("문제를 찾을 수 없습니다. question_id=" + questionId);
        }
        
        // 2. item_id로 ItemMetadata와 HTML 데이터 조회
        ItemMetadata metadata = reportRepository.findItemMetadataWithHtmlData(userExamItem.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("문항 정보를 찾을 수 없습니다. item_id=" + userExamItem.getItemId()));
        
        // 3. ItemSearchResponse로 변환
        ItemSearchResponse response = convertToItemSearchResponse(metadata);
        
        // 4. 선택지 HTML 데이터 별도 조회 및 설정 (리플렉션 사용)
        if (metadata.getHasHtmlData()) {
            ItemHtmlData htmlData = itemHtmlDataRepository.findByItemId(userExamItem.getItemId())
                    .orElse(null);
            if (htmlData != null) {
                setChoiceHtmlToResponse(response, htmlData);
                // passageHtml이 있는 경우에만 설정
                if (htmlData.getPassageHtml() != null && !htmlData.getPassageHtml().trim().isEmpty()) {
                    response.setPassageHtml(htmlData.getPassageHtml());
                }
            }
        }
        
        return response;
    }

    /**
     * ItemMetadata를 ItemSearchResponse로 변환
     */
    private ItemSearchResponse convertToItemSearchResponse(ItemMetadata metadata) {
        ItemSearchResponse.ItemSearchResponseBuilder builder = ItemSearchResponse.builder()
                .itemId(metadata.getItemId())
                .subjectId(metadata.getSubject() != null ? metadata.getSubject().getSubjectId() : null)
                .subjectName(metadata.getSubject() != null ? metadata.getSubject().getSubjectName() : null)
                .hasImageData(metadata.getHasImageData())
                .hasHtmlData(metadata.getHasHtmlData())
                .questionForm(metadata.getQuestionForm())
                .difficulty(metadata.getDifficulty())
                .chapterHierarchy(metadata.getChapterHierarchy())
                .passageId(metadata.getPassageId())
                .createdDate(metadata.getCreatedDate())
                .updatedDate(metadata.getUpdatedDate());

        // 이미지 데이터 처리
        if (metadata.getHasImageData() && metadata.getImageData() != null) {
            ItemImageData imageData = metadata.getImageData();
            builder.questionImageUrl(imageData.getQuestionUrl())
                    .answerImageUrl(imageData.getAnswerUrl())
                    .explainImageUrl(imageData.getExplainUrl())
                    .passageImageUrl(imageData.getPassageUrl());
        }

        // HTML 데이터 처리
        if (metadata.getHasHtmlData() && metadata.getHtmlData() != null) {
            ItemHtmlData htmlData = metadata.getHtmlData();
            builder.questionHtml(htmlData.getQuestionHtml())
                    .answerHtml(htmlData.getAnswerHtml())
                    .explainHtml(htmlData.getExplainHtml())
                    .passageHtml(htmlData.getPassageHtml());
        }

        return builder.build();
    }

    /**
     * 선택지 HTML 데이터를 ItemSearchResponse에 설정
     */
    private void setChoiceHtmlToResponse(ItemSearchResponse response, ItemHtmlData htmlData) {
        // ItemSearchResponse에 선택지 HTML 필드가 추가되었는지 확인 후 설정
        if (htmlData != null) {
            response.setChoice1Html(htmlData.getChoice1Html());
            response.setChoice2Html(htmlData.getChoice2Html());
            response.setChoice3Html(htmlData.getChoice3Html());
            response.setChoice4Html(htmlData.getChoice4Html());
            response.setChoice5Html(htmlData.getChoice5Html());
        }
    }

    @Override
    public List<DetailErrataResponse> findDetailErrataByExamId(Long examId, Long userId) {
        return reportRepository.findDetailErrata(examId, userId)
                .stream()
                .map(DetailErrataResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<DetailDifficultyResponse> findDetailDifficultyByExamId(Long userId, Long examId) {

        return reportRepository.findDetailDifficultyByExamId(userId, examId).stream()
                .map(DetailDifficultyResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<DetailEvaluationResponse> findDetailEvaluationByExamId(Long userId, Long examId) {
        return reportRepository.findDetailEvaluationByExamId(userId, examId).stream()
                .map(DetailEvaluationResponse::from)
                .collect(Collectors.toList());
    }

}
