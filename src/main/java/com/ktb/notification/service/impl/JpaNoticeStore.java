package com.ktb.notification.service.impl;

import com.ktb.notification.domain.Notice;
import com.ktb.notification.repository.NoticeRepository;
import com.ktb.notification.service.NoticeStore;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaNoticeStore implements NoticeStore {

    private final NoticeRepository repository;

    @Override
    public Page<Notice> findPublished(Pageable pageable) {
        return repository.findPublished(pageable);
    }

    @Override
    public Page<Notice> findAllActive(Pageable pageable) {
        return repository.findAllActive(pageable);
    }

    @Override
    public Optional<Notice> findActiveById(Long id) {
        return repository.findActiveById(id);
    }

    @Override
    public Notice save(Notice notice) {
        return repository.save(notice);
    }
}
