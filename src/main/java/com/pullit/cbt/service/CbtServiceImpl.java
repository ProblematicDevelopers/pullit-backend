package com.pullit.cbt.service;

import com.pullit.cbt.dto.request.CbtExamCreateRequest;
import com.pullit.exam.entity.UserExam;
import com.pullit.exam.entity.UserExamItem;
import com.pullit.exam.enums.ExamVisibility;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.item.dao.ItemMetadataRepository;
import com.pullit.item.dao.SubjectRepository;
import com.pullit.item.dto.response.SubjectResponse;
import com.pullit.item.entity.ItemMetadata;
import com.pullit.item.entity.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CbtServiceImpl implements CbtService {
    private final SubjectRepository subjectRepository;
    private final UserExamRepository userExamRepository;
    private final ItemMetadataRepository itemMetadataRepository;

    @Override
    public Long createExam(Long userId, CbtExamCreateRequest request) {
        Long subjectId = request.getSubjectId();
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("과목을 찾을 수 없습니다. id=" + subjectId));
        SubjectResponse s = SubjectResponse.from(subject);

        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formatted = sdf.format(today);

        String examName = "[CBT]" + s.getSchoolLevelName() + " > " + s.getAreaName() + " > " + s.getSubjectName() + " "
                + formatted;
        UserExam exam = UserExam.builder()
                .examName(examName)
                .examType("CBT")
                .totalItems(request.getQuestionCount())
                .timeLimit(request.getTimeLimit())
                .gradeCode(s.getGradeCode())
                .gradeName(s.getGradeName())
                .termCode(s.getTermCode())
                .termName(s.getTermName())
                .areaName(s.getAreaName())
                .areaCode(s.getAreaCode())
                .visibility(ExamVisibility.PRIVATE)
                .build();
        try {
            UserExam u = userExamRepository.save(exam);
            return u.getId();
        } catch (Exception e) {
            // 저장에 실패한 경우 예외 메시지와 함께 null 반환 또는 예외 재던지기
            // 필요에 따라 로그를 남길 수도 있음
            throw new RuntimeException("시험 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public void addExamItem(Long examId, CbtExamCreateRequest request) {
        // 문제를 난이도별로 고루 분포하게 뽑아서 UserExamItem으로 추가하는 로직
        // (1) ItemMetadata에서 largeChapterId, mediumChapters 조건에 맞는 문제 추출
        // (2) 난이도별로 분포 고려하여 request.getQuestionCount()만큼 선정
        // (3) UserExamItem 생성 후 UserExam에 추가

        // 필요한 repository, entity import는 클래스 상단에서 처리되어 있다고 가정

        // 1. 시험 엔티티 조회
        UserExam userExam = userExamRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + examId));

        // 2. ItemMetadataRepository 필요 (여기서는 itemMetadataRepository가 주입되어 있다고 가정)
        // 실제 환경에서는 @Autowired 또는 생성자 주입 필요
        // 예시: private final ItemMetadataRepository itemMetadataRepository;

        // 3. 요청에서 chapter 조건 추출
        List<CbtExamCreateRequest.SelectedChapter> selectedChapters = request.getSelectedChapters();
        int totalCount = request.getQuestionCount();

        // 4. 조건에 맞는 모든 문제 조회
        List<Long> mediumChapterCodes = new java.util.ArrayList<>();
        List<Long> largeChapterCodes = new java.util.ArrayList<>();
        for (CbtExamCreateRequest.SelectedChapter chapter : selectedChapters) {
            if (chapter.getLargeChapterId() != null && !chapter.getLargeChapterId().isEmpty()) {
                try {
                    largeChapterCodes.add(Long.parseLong(chapter.getLargeChapterId()));
                } catch (NumberFormatException ignore) {
                }
            }
            if (chapter.getMediumChapters() != null) {
                mediumChapterCodes.addAll(chapter.getMediumChapters());
            }
        }

        // ItemMetadataRepository에서 조건에 맞는 문제 조회
        List<ItemMetadata> candidates = itemMetadataRepository
                .findBySubject_SubjectIdAndChapterHierarchy_LargeChapter_CodeInAndChapterHierarchy_MediumChapter_CodeIn(
                        request.getSubjectId(), largeChapterCodes, mediumChapterCodes);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("조건에 맞는 문제가 없습니다.");
        }

        // 5. 난이도별로 분포 계산
        // 난이도 코드 추출 (예: "EASY", "MEDIUM", "HARD" 등)
        Map<Long, List<ItemMetadata>> difficultyMap = new java.util.HashMap<>();
        for (ItemMetadata item : candidates) {
            Long diff = (item.getDifficulty() != null && item.getDifficulty().getCode() != null)
                    ? item.getDifficulty().getCode()
                    : -1L;
            difficultyMap.computeIfAbsent(diff, k -> new java.util.ArrayList<>()).add(item);
        }

        // 난이도별로 균등 분포 (남은 문제는 랜덤하게 채움)
        List<ItemMetadata> selectedItems = new java.util.ArrayList<>();
        int diffTypes = difficultyMap.size();
        int baseCount = totalCount / diffTypes;
        int remain = totalCount % diffTypes;

        java.util.Random random = new java.util.Random();
        for (List<ItemMetadata> diffList : difficultyMap.values()) {
            java.util.Collections.shuffle(diffList, random);
        }

        // 각 난이도에서 baseCount만큼 뽑기
        for (List<ItemMetadata> diffList : difficultyMap.values()) {
            int pick = Math.min(baseCount, diffList.size());
            selectedItems.addAll(diffList.subList(0, pick));
        }

        // 남은 개수는 난이도 상관없이 랜덤하게 채움
        List<ItemMetadata> remainPool = new java.util.ArrayList<>();
        for (List<ItemMetadata> diffList : difficultyMap.values()) {
            if (diffList.size() > baseCount) {
                remainPool.addAll(diffList.subList(baseCount, diffList.size()));
            }
        }
        java.util.Collections.shuffle(remainPool, random);
        for (int i = 0; i < remain && i < remainPool.size(); i++) {
            selectedItems.add(remainPool.get(i));
        }

        // 만약 문제 수가 부족하면 candidates에서 랜덤하게 채움
        while (selectedItems.size() < totalCount && selectedItems.size() < candidates.size()) {
            ItemMetadata extra = candidates.get(random.nextInt(candidates.size()));
            if (!selectedItems.contains(extra)) {
                selectedItems.add(extra);
            }
        }

        // 6. UserExamItem 생성 및 시험에 추가
        int order = userExam.getExamItems() != null ? userExam.getExamItems().size() + 1 : 1;
        for (ItemMetadata item : selectedItems) {
            UserExamItem examItem = UserExamItem.builder()
                    .userExam(userExam)
                    .itemId(item.getItemId())
                    .subjectId(item.getSubject().getSubjectId())
                    .itemOrder(order++)
                    .points(null) // 배점은 필요시 설정
                    .build();
            userExam.addExamItem(examItem);
        }

        // 7. 저장
        userExamRepository.save(userExam);
    }

}
