package com.ktb.portfolio.repository;

import com.ktb.portfolio.domain.Portfolio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByAccount_IdAndDeletedAtIsNull(Long accountId);

    @EntityGraph(attributePaths = {
            "projects",
            "projects.architectureImage"
    })
    Optional<Portfolio> findWithProjectsByAccount_IdAndDeletedAtIsNull(Long accountId);

    @EntityGraph(attributePaths = {
            "projects",
            "projects.architectureImage"
    })
    Optional<Portfolio> findWithProjectsByAccount_Id(Long accountId);
}
