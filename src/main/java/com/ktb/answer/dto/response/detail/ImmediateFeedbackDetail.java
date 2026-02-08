package com.ktb.answer.dto.response.detail;

import com.ktb.answer.dto.response.common.KeywordCheck;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "즉각 피드백 상세")
public record ImmediateFeedbackDetail(
    @Schema(description = "키워드 체크 결과 목록")
    List<KeywordCheck> keywords
) {
}
