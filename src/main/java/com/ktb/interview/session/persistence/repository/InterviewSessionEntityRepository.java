package com.ktb.interview.session.persistence.repository;

import com.ktb.interview.session.persistence.entity.InterviewSessionEntity;
import com.ktb.interview.session.persistence.repository.projection.InterviewSessionFinalFeedbackReadModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewSessionEntityRepository extends JpaRepository<InterviewSessionEntity, String> {
    Optional<InterviewSessionEntity> findBySessionIdAndAccountId(String sessionId, Long accountId);

    /**
     * 세션 최종 피드백 조회 응답을 단일 round-trip으로 제공하기 위한 read projection.
     * metrics/topics/history는 각각 jsonb_agg 서브쿼리로 집계됩니다.
     */
    @Query(
            value = """
                    SELECT
                        s.session_id AS sessionId,
                        s.account_id AS accountId,
                        s.interview_type AS interviewType,
                        s.initial_question_id AS initialQuestionId,
                        s.session_status_cd AS sessionStatus,
                        f.session_id AS feedbackSessionId,
                        f.bad_case_type_cd AS badCaseType,
                        f.bad_case_message AS badCaseMessage,
                        f.bad_case_guidance AS badCaseGuidance,
                        f.keyword_covered_json::text AS keywordCoveredJson,
                        f.keyword_missing_json::text AS keywordMissingJson,
                        f.keyword_coverage_ratio AS keywordCoverageRatio,
                        f.overall_strengths_feedback AS overallStrengths,
                        f.overall_improvements_feedback AS overallImprovements,
                        COALESCE(
                            (
                                SELECT jsonb_agg(
                                           jsonb_build_object(
                                               'name', COALESCE(m.metric_nm, sm.metric_id::text),
                                               'score', sm.metric_score
                                           )
                                           ORDER BY sm.metric_id
                                       )
                                FROM interview_session_metric sm
                                LEFT JOIN metric m ON m.metric_id = sm.metric_id
                                WHERE sm.session_id = s.session_id
                            ),
                            '[]'::jsonb
                        )::text AS metricsJson,
                        COALESCE(
                            (
                                SELECT jsonb_agg(
                                           jsonb_build_object(
                                               'topic_id', tf.topic_id,
                                               'main_question', tf.main_question_text,
                                               'strengths', tf.strengths_feedback,
                                               'improvements', tf.improvements_feedback
                                           )
                                           ORDER BY tf.topic_id
                                       )
                                FROM interview_topic_feedback tf
                                WHERE tf.session_id = s.session_id
                            ),
                            '[]'::jsonb
                        )::text AS topicsJson,
                        COALESCE(
                            (
                                SELECT jsonb_agg(
                                           jsonb_build_object(
                                               'question', t.question_text,
                                               'category', t.question_ctg,
                                               'answer_text', t.answer_text,
                                               'turn_type', t.turn_type_cd,
                                               'turn_order', t.turn_order,
                                               'topic_id', t.topic_id,
                                               'video_file_id', t.video_file_id
                                           )
                                           ORDER BY t.turn_order
                                       )
                                FROM interview_turn t
                                WHERE t.session_id = s.session_id
                            ),
                            '[]'::jsonb
                        )::text AS historyJson
                    FROM interview_session s
                    LEFT JOIN interview_session_feedback f ON f.session_id = s.session_id
                    WHERE s.session_id = :sessionId
                    """,
            nativeQuery = true
    )
    Optional<InterviewSessionFinalFeedbackReadModel> findFinalFeedbackReadModelBySessionId(
            @Param("sessionId") String sessionId
    );
}
