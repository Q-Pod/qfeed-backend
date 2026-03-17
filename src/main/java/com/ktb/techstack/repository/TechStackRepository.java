package com.ktb.techstack.repository;

import com.ktb.techstack.domain.TechStack;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TechStackRepository extends JpaRepository<TechStack, Long> {

    @Query("""
            SELECT ts FROM TechStack ts
            WHERE ts.useYn = true
            AND (:keyword IS NULL OR LOWER(ts.name) LIKE CONCAT('%', :keyword, '%'))
            AND (:cursor IS NULL OR ts.id < :cursor)
            ORDER BY ts.id DESC
            """)
    Slice<TechStack> searchActive(
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    List<TechStack> findAllByIdInAndUseYnTrue(Collection<Long> ids);
}
