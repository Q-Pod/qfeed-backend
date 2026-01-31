package com.ktb.answer.service;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.AnswerDetailQuery;
import com.ktb.answer.dto.AnswerDetailResult;
import com.ktb.answer.dto.AnswerSubmitCommand;
import com.ktb.answer.dto.AnswerSubmitResult;
import com.ktb.answer.dto.FeedbackResult;
import com.ktb.answer.dto.response.AnswerListResponse;
import com.ktb.answer.exception.AnswerAccessDeniedException;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.file.exception.FileAlreadyDeletedException;
import com.ktb.file.exception.FileExtensionNotAllowedException;
import com.ktb.file.exception.FileNotFoundException;
import com.ktb.file.exception.FileSizeExceededException;
import com.ktb.file.exception.FileStorageMigrationException;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.exception.QuestionDisabledException;
import com.ktb.question.exception.QuestionNotFoundException;
import java.time.LocalDate;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public interface AnswerApplicationService {

    /**
     * 답변 목록 조회 (본인 답변만)
     *
     * @param accountId       현재 사용자 ID
     * @param type            답변 타입 필터 (선택)
     * @param category        질문 카테고리 필터 (선택)
     * @param questionType    질문 타입 필터 (선택)
     * @param dateFrom        시작 날짜 (선택)
     * @param dateTo          종료 날짜 (선택)
     * @param cursor          페이지네이션 커서 (Base64)
     * @param limit           조회 개수
     * @return 답변 목록 (Slice)
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
     * 답변 제출 (연습/단일 답변)
     *
     * @throws QuestionNotFoundException        질문이 존재하지 않는 경우 (404)
     * @throws QuestionDisabledException        질문이 비활성화된 경우 (400)
     * @throws FileSizeExceededException        파일 크기가 초과된 경우 (422)
     * @throws FileExtensionNotAllowedException 지원하지 않는 파일 형식 (400)
     * @throws FileNotFoundException            업로드에 참조된 파일을 찾을 수 없는 경우 (404)
     * @throws FileAlreadyDeletedException      업로드 파일이 이미 삭제된 경우 (409)
     * @throws FileStorageMigrationException    저장소 이동 중 장애가 발생한 경우 (422)
     */
    @Transactional
    AnswerSubmitResult submit(Long accountId, AnswerSubmitCommand command)
            throws QuestionNotFoundException, QuestionDisabledException,
            FileSizeExceededException, FileExtensionNotAllowedException,
            FileNotFoundException, FileAlreadyDeletedException, FileStorageMigrationException;

    /**
     * 세션 기반 답변 제출 (실전 모드)
     *
     * @param accountId 현재 사용자 ID
     * @param sessionId 세션 ID
     * @param command   답변 제출 정보
     * @return 답변 제출 결과
     * @throws QuestionNotFoundException 질문이 존재하지 않는 경우
     * TODO: 세션 관련 Exception 추가 필요
     */
    @Transactional
    AnswerSubmitResult submitWithSession(Long accountId, String sessionId, AnswerSubmitCommand command)
            throws QuestionNotFoundException;

    /**
     * 답변 상세 조회
     *
     * @throws AnswerNotFoundException     답변이 존재하지 않는 경우 (404)
     * @throws AnswerAccessDeniedException 본인 답변이 아닌 경우 (403)
     */
    AnswerDetailResult getDetail(Long accountId, Long answerId, AnswerDetailQuery query)
            throws AnswerNotFoundException, AnswerAccessDeniedException;

    /**
     * AI 피드백 조회
     *
     * @throws AnswerNotFoundException     답변이 존재하지 않는 경우 (404)
     * @throws AnswerAccessDeniedException 본인 답변이 아닌 경우 (403)
     */
    FeedbackResult getFeedback(Long accountId, Long answerId)
            throws AnswerNotFoundException, AnswerAccessDeniedException;
}
