package com.ktb.interview.session.persistence.repository;

import com.ktb.interview.session.persistence.entity.InterviewTopicFeedbackEntity;
import com.ktb.interview.session.persistence.entity.InterviewTopicFeedbackId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewTopicFeedbackEntityRepository extends JpaRepository<InterviewTopicFeedbackEntity, InterviewTopicFeedbackId> {
    void deleteByIdSessionId(String sessionId);

    List<InterviewTopicFeedbackEntity> findByIdSessionIdOrderByIdTopicIdAsc(String sessionId);
}
