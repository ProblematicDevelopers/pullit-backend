package com.pullit.cbt.dto.response;

import com.pullit.cbt.entity.AttemptExam;
import com.pullit.user.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptExamResponse {

    private Long id;
    private UserResponse user;
    private Long examId;
    private String examName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private AttemptExam.AttemptStatus status;
    private List<AttemptExamQuestionResponse> attemptQuestions;

}
