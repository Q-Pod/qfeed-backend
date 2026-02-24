package com.ktb.interview.session.persistence.repository;

import com.ktb.interview.session.persistence.entity.InterviewTurnEntity;
import com.ktb.interview.session.persistence.entity.InterviewTurnId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewTurnEntityRepository extends JpaRepository<InterviewTurnEntity, InterviewTurnId> {
    void deleteByIdSessionId(String sessionId);

    List<InterviewTurnEntity> findByIdSessionIdOrderByIdTurnOrderAsc(String sessionId);
}
