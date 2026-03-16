package com.ktb.portfolio.service;

import com.ktb.portfolio.dto.request.PortfolioUpsertRequest;
import com.ktb.portfolio.dto.response.PortfolioResponse;

public interface PortfolioService {

    PortfolioResponse getMyPortfolio(Long accountId);

    PortfolioResponse upsertMyPortfolio(Long accountId, PortfolioUpsertRequest request);

    Long deleteMyPortfolio(Long accountId);

}
