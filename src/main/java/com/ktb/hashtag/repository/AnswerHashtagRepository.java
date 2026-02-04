package com.ktb.hashtag.repository;

import com.ktb.hashtag.domain.AnswerHashtag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerHashtagRepository extends JpaRepository<AnswerHashtag, Long> {

    /**
     * 답변 삭제 시 AnswerHashtag 함께 삭제
     */
    void deleteByAnswerId(Long answerId);

    @Query("""
            SELECT ah FROM AnswerHashtag ah
            JOIN FETCH ah.hashtag h
            WHERE ah.answer.id = :answerId
            ORDER BY h.id ASC
            """)
    List<AnswerHashtag> findByAnswerIdWithHashtag(@Param("answerId") Long answerId);
}
