package com.ktb.techstack.service.impl;

import com.ktb.techstack.domain.TechStack;
import com.ktb.techstack.dto.TechStackListResponse;
import com.ktb.techstack.dto.TechStackPaginationResponse;
import com.ktb.techstack.dto.TechStackResponse;
import com.ktb.techstack.repository.TechStackRepository;
import java.util.Locale;
import com.ktb.techstack.service.TechStackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TechStackServiceImpl implements TechStackService {

    private final TechStackRepository techStackRepository;

    @Override
    @Transactional(readOnly = true)
    public TechStackListResponse getTechStacks(String keyword, Long cursor, int size) {
        String normalizedKeyword = normalizeKeyword(keyword);
        PageRequest pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));
        Slice<TechStack> techStacks = techStackRepository.searchActive(normalizedKeyword, cursor, pageable);

        return new TechStackListResponse(
                techStacks.getContent().stream()
                        .map(techStack -> new TechStackResponse(
                                techStack.getId(),
                                techStack.getName(),
                                techStack.getDescription()
                        ))
                        .toList(),
                toPaginationResponse(techStacks)
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private TechStackPaginationResponse toPaginationResponse(Slice<TechStack> slice) {
        Long nextCursor = null;
        if (!slice.getContent().isEmpty()) {
            TechStack last = slice.getContent().get(slice.getContent().size() - 1);
            nextCursor = slice.hasNext() ? last.getId() : null;
        }
        return new TechStackPaginationResponse(nextCursor, slice.hasNext(), slice.getSize());
    }
}
