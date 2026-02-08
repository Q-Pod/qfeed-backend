package com.ktb.answer.service.impl;

import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.exception.AnswerAccessDeniedException;
import com.ktb.answer.exception.AnswerInvalidContentException;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.answer.exception.DuplicateAnswerException;
import com.ktb.answer.exception.InvalidAnswerContentException;
import com.ktb.answer.exception.InvalidAnswerStatusTransitionException;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.question.domain.Question;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerDomainServiceImpl implements AnswerDomainService {

    private final QuestionRepository questionRepository;
    private final UserAccountRepository userAccountRepository;

    private static final int MAX_ANSWER_CONTENT_LENGTH = 1_500;

    @Override
    public Answer createAnswer(Long accountId, Long questionId, String answerContent, AnswerType type) {
        log.debug("Creating answer for accountId: {}, questionId: {}", accountId, questionId);

        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        return Answer.create(question, account, answerContent, type);
    }

    @Override
    public void validateOwnership(Answer answer, Long accountId)
            throws AnswerAccessDeniedException {
        log.debug("Validating ownership for answerId: {}, accountId: {}", answer.getId(), accountId);

        if (!answer.isOwnedBy(accountId)) {
            throw new AnswerAccessDeniedException(answer.getId(), accountId);
        }
    }

    @Override
    public void transitionStatus(Answer answer, AnswerStatus nextStatus)
            throws InvalidAnswerStatusTransitionException {
        log.debug("Transitioning answer status: answerId={}, from={}, to={}",
                answer.getId(), answer.getStatus(), nextStatus);

        answer.transitionTo(nextStatus);
    }

    @Override
    public void checkDuplicateAnswer(String sessionId, Long questionId)
            throws DuplicateAnswerException {
        log.debug("Checking duplicate answer for sessionId: {}, questionId: {}", sessionId, questionId);

        // MVP V2: ANSWER_SESSION 엔티티 구현 후 활성화
    }

    @Override
    public void validateAnswerContent(String answerText)
            throws InvalidAnswerContentException {
        log.debug("Validating answer content: hasText={}",
                answerText != null && !answerText.isBlank());

        boolean hasText = answerText != null && !answerText.isBlank();

        if (!hasText) {
            throw new AnswerInvalidContentException();
        }

        if (answerText.length() > MAX_ANSWER_CONTENT_LENGTH) {
            throw new AnswerInvalidContentException();
        }
    }
}
