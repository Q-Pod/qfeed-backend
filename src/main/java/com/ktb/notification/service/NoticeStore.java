package com.ktb.notification.service;

import com.ktb.notification.domain.Notice;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeStore {

    Page<Notice> findPublished(Pageable pageable);

    Page<Notice> findAllActive(Pageable pageable);

    Optional<Notice> findActiveById(Long id);

    Notice save(Notice notice);
}
