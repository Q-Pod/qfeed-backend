package com.ktb.ai.stt.service;

/**
 * STT 서비스 인터페이스
 *
 * <p>Speech-to-Text 변환 기능 제공
 */
public interface SttService {

    /**
     * 음성 파일 → 텍스트 변환
     *
     * @param userId    사용자 ID
     * @param sessionId 세션 ID
     * @param audioUrl  S3 오디오 파일 URL
     * @return 변환된 텍스트
     * @throws com.ktb.ai.stt.exception.SttServiceException           STT 서버 호출 실패 시
     * @throws com.ktb.ai.stt.exception.AudioTooLongException         오디오 길이 초과
     * @throws com.ktb.ai.stt.exception.AudioTooLargeException        파일 크기 초과
     * @throws com.ktb.ai.stt.exception.AudioNotFoundException        오디오 파일 없음
     * @throws com.ktb.ai.stt.exception.AudioUnprocessableException   오디오 처리 불가
     * @throws com.ktb.ai.stt.exception.SttTimeoutException           변환 시간 초과
     */
    String convertToText(Long userId, String sessionId, String audioUrl);
}
