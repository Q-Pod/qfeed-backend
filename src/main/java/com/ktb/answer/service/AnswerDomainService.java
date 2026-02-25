package com.ktb.answer.service;

import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.AnswerDetailResult;
import com.ktb.answer.dto.response.list.AnswerListResponse;
import com.ktb.answer.exception.AnswerAccessDeniedException;
import com.ktb.answer.exception.DuplicateAnswerException;
import com.ktb.answer.exception.InvalidAnswerContentException;
import com.ktb.answer.exception.InvalidAnswerStatusTransitionException;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.exception.QuestionNotFoundException;
import java.time.LocalDate;

public interface AnswerDomainService {
    /**
     * 답변 생성
     * @return 생성된 Answer 엔티티
     * @throws AccountNotFoundException     제출한 유저가 존재하지 않을 때(404)
     * @throws QuestionNotFoundException    제출한 답변의 문제가 존재하지 않을 때(404)
     */
    Answer createAnswer(Long accountId, Long questionId, String answerContent, AnswerType type);

    /**
     * 답변 소유권 검증
     * @throws AnswerAccessDeniedException 본인 답변이 아닌 경우
     */
    void validateOwnership(Answer answer, Long accountId) throws AnswerAccessDeniedException;

    /**
     * 답변 상태 전이
     * @throws InvalidAnswerStatusTransitionException 허용되지 않는 상태 전이인 경우
     */
    void transitionStatus(Answer answer, AnswerStatus nextStatus) throws InvalidAnswerStatusTransitionException;

    /**
     * 중복 답변 검증 (세션 내 동일 질문)
     * @throws DuplicateAnswerException 이미 답변이 존재하는 경우
     */
    void checkDuplicateAnswer(String sessionId, Long questionId) throws DuplicateAnswerException;

    /**
     * 답변 텍스트 유효성 검증
     * @throws InvalidAnswerContentException 답변 내용이 유효하지 않은 경우
     */
    void validateAnswerContent(String answerText) throws InvalidAnswerContentException;

    /**
     * 답변 목록 조회 (본인 답변만)
     */
    AnswerListResponse getList(
            Long accountId,
            AnswerType type,
            QuestionCategory category,
            QuestionType questionType,
            LocalDate dateFrom,
            LocalDate dateTo,
            String cursor,
            Integer limit
    );

    /**
     * 답변 상세 조회 (본인 답변만)
     */
    AnswerDetailResult getDetail(Long accountId, Long answerId);
}
