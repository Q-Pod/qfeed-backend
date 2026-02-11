package com.ktb.notification.repository;

import com.ktb.notification.domain.Notice;
import com.ktb.notification.domain.enums.NoticeStatusCd;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notice> findAllActive();

    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL")
    Page<Notice> findAllActive(Pageable pageable);

    @Query("SELECT n FROM Notice n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<Notice> findActiveById(@Param("id") Long id);

    @Query("SELECT n FROM Notice n WHERE n.status = :status AND n.deletedAt IS NULL ORDER BY n.publishedAt DESC")
    List<Notice> findByStatus(@Param("status") NoticeStatusCd status);

    @Query("SELECT n FROM Notice n WHERE n.status = :status AND n.deletedAt IS NULL")
    Page<Notice> findByStatus(@Param("status") NoticeStatusCd status, Pageable pageable);

    @Query("SELECT n FROM Notice n WHERE n.status = 'PUBLISHED' AND n.deletedAt IS NULL ORDER BY n.publishedAt DESC")
    Page<Notice> findPublished(Pageable pageable);
}
