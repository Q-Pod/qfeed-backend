package com.ktb.common.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.Parameter;

/*
 * AWS SSM Parameter Store에서 값을 읽어와 Spring Environment에 주입하는 부트스트랩 로더.
 * - 실행 시점: ApplicationContext 생성 전(아주 초기 단계)
 * - 목적: application.yaml의 ${ENV_VAR}가 SSM 값을 참조할 수 있게 함
 */
public class ParameterStoreEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final Logger log = LoggerFactory.getLogger(ParameterStoreEnvironmentPostProcessor.class);
    private static final String SOURCE_NAME = "aws-parameter-store";
    private static final String[] PARAMETER_STORE_PATH_KEYS = {
        "AWS_PARAMETER_STORE_PATH",
        "PARAMETER_STORE_PATH",
        "aws.parameter-store.path"
    };
    private static final String[] PARAMETER_STORE_ENABLED_KEYS = {
        "AWS_PARAMETER_STORE_ENABLED",
        "PARAMETER_STORE_ENABLED",
        "aws.parameter-store.enabled"
    };
    private static final String[] AWS_REGION_KEYS = {"AWS_REGION", "aws.region"};

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        /*
         * Parameter Store를 사용할지 판단하기 위한 경로 설정.
         * 이 값은 "아직" application.yaml 바인딩 전이므로,
         * 시스템 환경변수/시스템 프로퍼티에 미리 존재해야 안정적으로 동작한다.
         */
        String path = resolve(environment, PARAMETER_STORE_PATH_KEYS);
        if (!StringUtils.hasText(path)) {
            // 경로가 없으면 Parameter Store를 사용하지 않는 것으로 보고 종료.
            return;
        }

        boolean enabled = resolveBoolean(environment, true, PARAMETER_STORE_ENABLED_KEYS);
        if (!enabled) {
            // 명시적으로 비활성화된 경우 종료.
            return;
        }

        // SSM 호출에 필요한 리전. 없으면 경고만 남기고 앱 기동은 계속한다.
        String region = resolve(environment, AWS_REGION_KEYS);
        if (!StringUtils.hasText(region)) {
            log.warn("AWS Parameter Store enabled but region is missing. Set AWS_REGION or aws.region.");
            return;
        }

        String normalizedPath = normalizePath(path);
        Map<String, Object> values = new HashMap<>();

        /*
         * 기본 자격증명 체인 사용:
         * - EC2: 인스턴스 프로파일(IAM Role)
         * - 로컬: 환경변수 / AWS_PROFILE / ~/.aws/credentials
         */
        try (SsmClient ssm = SsmClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()) {
            String nextToken = null;
            do {
                // 지정한 경로 하위의 파라미터를 재귀적으로 조회 (SecureString 복호화 포함)
                GetParametersByPathRequest request = GetParametersByPathRequest.builder()
                    .path(normalizedPath)
                    .recursive(true)
                    .withDecryption(true)
                    .nextToken(nextToken)
                    .build();
                var response = ssm.getParametersByPath(request);
                for (Parameter parameter : response.parameters()) {
                    // 예: "/qfeed/prod/jwt/secret" -> "jwt.secret"
                    String key = toPropertyKey(normalizedPath, parameter.name());
                    if (StringUtils.hasText(key)) {
                        values.putIfAbsent(key, parameter.value());
                    }
                }
                nextToken = response.nextToken();
            } while (StringUtils.hasText(nextToken));
        } catch (Exception ex) {
            // 실패해도 서비스는 계속 기동 (Parameter Store는 옵션 소스로 취급)
            log.warn("Failed to load AWS Parameter Store values from {}", normalizedPath, ex);
            return;
        }

        if (!values.isEmpty()) {
            // 시스템 환경변수보다 뒤에 붙여서 ENV > SSM > yaml 우선순위 유지
            addPropertySource(environment.getPropertySources(), new MapPropertySource(SOURCE_NAME, values));
            log.info("Loaded {} values from AWS Parameter Store path {}", values.size(), normalizedPath);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static String resolve(ConfigurableEnvironment environment, String... keys) {
        // 여러 키를 순서대로 조회해서 가장 먼저 발견되는 값을 사용
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean resolveBoolean(ConfigurableEnvironment environment, boolean defaultValue, String... keys) {
        // 값이 없으면 defaultValue를 사용
        String value = resolve(environment, keys);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static String normalizePath(String path) {
        // "/qfeed/prod/" 같은 입력을 "/qfeed/prod"로 정규화
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String toPropertyKey(String basePath, String fullName) {
        // basePath 제거 후 "/" -> "." 치환
        String key = fullName;
        if (key.startsWith(basePath)) {
            key = key.substring(basePath.length());
        }
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (!StringUtils.hasText(key)) {
            return "";
        }
        return key.replace("/", ".");
    }

    private static void addPropertySource(MutablePropertySources sources, MapPropertySource source) {
        // 시스템 환경변수 뒤에 추가해 ENV 우선순위를 유지
        if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, source);
        } else {
            sources.addLast(source);
        }
    }
}
