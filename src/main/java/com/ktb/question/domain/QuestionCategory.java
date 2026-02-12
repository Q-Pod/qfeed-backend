package com.ktb.question.domain;

import java.util.EnumSet;
import java.util.List;
import lombok.Getter;

/**
 * 질문 카테코리 ENUM
 */
@Getter
public enum QuestionCategory {
    // CS
    OS("운영체제", QuestionType.CS),
    NETWORK("네트워크", QuestionType.CS),
    DB("데이터베이스", QuestionType.CS),
    COMPUTER_ARCHITECTURE("컴퓨터 구조", QuestionType.CS),
    DATA_STRUCTURE_ALGORITHM("자료구조&알고리즘", QuestionType.CS),

    // SYSTEM_DESIGN
    SOCIAL("소셜/피드 시스템", QuestionType.SYSTEM_DESIGN),
    NOTIFICATION("알림 시스템", QuestionType.SYSTEM_DESIGN),
    REALTIME("실시간 통신 시스템", QuestionType.SYSTEM_DESIGN),
    SEARCH("검색 시스템", QuestionType.SYSTEM_DESIGN),
    MEDIA("미디어/스트리밍 시스템", QuestionType.SYSTEM_DESIGN),
    STORAGE("파일 저장/협업 시스템", QuestionType.SYSTEM_DESIGN),
    PLATFORM("플랫폼 인프라 시스템", QuestionType.SYSTEM_DESIGN),
    TRANSACTION("거래/정산 시스템", QuestionType.SYSTEM_DESIGN),

    // PORTFOLIO
    PORTFOLIO("포트폴리오", QuestionType.PORTFOLIO);

    private final String category;
    private final EnumSet<QuestionType> supportedTypes;

    QuestionCategory(String category, QuestionType... supportedTypes) {
        this.category = category;
        this.supportedTypes = EnumSet.noneOf(QuestionType.class);
        this.supportedTypes.addAll(List.of(supportedTypes));
    }

    public boolean supports(QuestionType type) {
        return supportedTypes.contains(type);
    }
}
