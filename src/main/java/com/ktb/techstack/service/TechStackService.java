package com.ktb.techstack.service;

import com.ktb.techstack.dto.TechStackListResponse;

public interface TechStackService {

    TechStackListResponse getTechStacks(String keyword, Long cursor, int size);
}
