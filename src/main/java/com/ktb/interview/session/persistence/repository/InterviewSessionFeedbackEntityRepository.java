package com.ktb.interview.session.persistence.repository;

import com.ktb.interview.session.persistence.entity.InterviewSessionFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionFeedbackEntityRepository extends JpaRepository<InterviewSessionFeedbackEntity, String> {
}
