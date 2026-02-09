package com.ktb.auth.security.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.security")
public class SecurityProperties {

    private List<String> permitAllEndpoints = new ArrayList<>();

    private List<String> anonymousOnlyEndpoints = new ArrayList<>();

    private Boolean isAuthenticated;

    // RequestMatcher 캐싱
    private RequestMatcher permitAllMatcher;
    private RequestMatcher anonymousOnlyMatcher;
    private RequestMatcher publicEndpointsMatcher;  // permitAll + anonymous 합친 것

    @PostConstruct
    public void init() {
        validateConfiguration();
        initializeMatchers();
        logConfiguration();
    }

    private void validateConfiguration() {
        if (permitAllEndpoints.isEmpty() && anonymousOnlyEndpoints.isEmpty()) {
            log.warn("Both permitAllEndpoints and anonymousOnlyEndpoints are empty. All endpoints may require authentication.");
        } else if (permitAllEndpoints.isEmpty()) {
            log.warn("permitAllEndpoints is empty. Public endpoints may require authentication (this can be intentional by environment).");
        } else if (anonymousOnlyEndpoints.isEmpty()) {
            log.warn("anonymousOnlyEndpoints is empty. Anonymous-only endpoints are disabled (this can be intentional by environment).");
        }

        Set<String> duplicates = permitAllEndpoints.stream()
                .filter(anonymousOnlyEndpoints::contains)
                .collect(Collectors.toSet());

        if (!duplicates.isEmpty()) {
            log.error("Duplicate endpoints found in both permitAll and anonymous: {}", duplicates);
        }
    }

    private void initializeMatchers() {
        this.permitAllMatcher = createOrRequestMatcher(permitAllEndpoints);

        this.anonymousOnlyMatcher = createOrRequestMatcher(anonymousOnlyEndpoints);

        // public endpoints (permitAll + anonymous)
        List<String> allPublicEndpoints = new ArrayList<>();
        allPublicEndpoints.addAll(permitAllEndpoints);
        allPublicEndpoints.addAll(anonymousOnlyEndpoints);
        this.publicEndpointsMatcher = createOrRequestMatcher(allPublicEndpoints);
    }

    private RequestMatcher createOrRequestMatcher(List<String> patterns) {
        if (patterns.isEmpty()) {
            return request -> false;  // 항상 false를 반환하는 Matcher
        }

        List<RequestMatcher> matchers = patterns.stream()
                .map(this::createRequestMatcher)
                .collect(Collectors.toList());

        return new OrRequestMatcher(matchers);
    }

    private RequestMatcher createRequestMatcher(String pattern) {
        if (pattern.startsWith("regex:")) {
            String regex = pattern.substring(6);
            return new RegexRequestMatcher(regex, null);
        }
        return PathPatternRequestMatcher.pathPattern(pattern);
    }

    /**
     * 설정된 엔드포인트 로깅
     */
    private void logConfiguration() {
        log.info("=== Security Endpoints Configuration ===");

        log.info("PermitAll Endpoints (accessible by everyone):");
        permitAllEndpoints.forEach(pattern -> log.info("  - {}", pattern));

        log.info("Anonymous Only Endpoints (accessible only when not authenticated):");
        anonymousOnlyEndpoints.forEach(pattern -> log.info("  - {}", pattern));

        log.info("Total Public Endpoints: {}",
                permitAllEndpoints.size() + anonymousOnlyEndpoints.size());
        log.info("========================================");
    }

    public String[] getPermitAllPatterns() {
        return permitAllEndpoints.toArray(new String[0]);
    }

    public String[] getAnonymousOnlyPatterns() {
        return anonymousOnlyEndpoints.toArray(new String[0]);
    }

    public String[] getAllPublicPatterns() {
        List<String> allPatterns = new ArrayList<>();
        allPatterns.addAll(permitAllEndpoints);
        allPatterns.addAll(anonymousOnlyEndpoints);
        return allPatterns.toArray(new String[0]);
    }

    public boolean isPermitAllEndpoint(HttpServletRequest request) {
        boolean matches = permitAllMatcher.matches(request);

        if (matches && log.isDebugEnabled()) {
            log.debug("Request {} matched permitAll endpoint", request.getRequestURI());
        }

        return matches;
    }

    public boolean isAnonymousOnlyEndpoint(HttpServletRequest request) {
        boolean matches = anonymousOnlyMatcher.matches(request);

        if (matches && log.isDebugEnabled()) {
            log.debug("Request {} matched anonymous only endpoint", request.getRequestURI());
        }

        return matches;
    }

    public boolean isPublicEndpoint(HttpServletRequest request) {
        boolean matches = publicEndpointsMatcher.matches(request);

        if (log.isDebugEnabled()) {
            String uri = request.getRequestURI();
            String method = request.getMethod();

            if (matches) {
                String type = isPermitAllEndpoint(request) ? "permitAll" : "anonymous";
                log.debug("Request [{}] {} matched public endpoint ({}) - SKIPPING JWT filter",
                        method, uri, type);
            } else {
                log.debug("Request [{}] {} requires authentication - APPLYING JWT filter",
                        method, uri);
            }
        }

        return matches;
    }
}
