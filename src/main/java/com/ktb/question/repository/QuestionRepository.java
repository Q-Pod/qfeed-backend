package com.ktb.question.repository;

import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
            SELECT q FROM Question q
            WHERE q.deletedAt IS NULL
            AND q.useYn = true
            AND (:type IS NULL OR q.type = :type)
            AND (:category IS NULL OR q.category = :category)
            AND (:cursor IS NULL OR q.id < :cursor)
            ORDER BY q.id DESC
            """)
    Slice<Question> findActiveByFilters(
            @Param("type") QuestionType type,
            @Param("category") QuestionCategory category,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            SELECT q FROM Question q
            WHERE q.deletedAt IS NULL
            AND q.useYn = true
            AND (:type IS NULL OR q.type = :type)
            AND (:category IS NULL OR q.category = :category)
            AND LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            AND (:cursor IS NULL OR q.id < :cursor)
            ORDER BY q.id DESC
            """)
    Slice<Question> searchActiveByKeyword(
            @Param("keyword") String keyword,
            @Param("type") QuestionType type,
            @Param("category") QuestionCategory category,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query(value = """
             SELECT question_id
             FROM question
             WHERE question_id >= (
                 SELECT floor(random() * (max(question_id) - min(question_id) + 1)) + min(question_id)
                 FROM question
                 WHERE use_yn = true AND deleted_at IS NULL 
             )
             AND use_yn = true
             AND deleted_at IS NULL
             ORDER BY question_id
             LIMIT 1
            """, nativeQuery = true)
    java.util.Optional<Long> findRandomActiveId();
}
