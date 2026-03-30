package com.ktb.common.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbPoolConfigLogger {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void logDbPoolConfig() {
        if (!(dataSource instanceof HikariDataSource hikari)) {
            log.info("DataSource type={}", dataSource.getClass().getName());
            return;
        }

        log.info(
            "[DB-POOL] type=Hikari, poolName={}, minIdle={}, maxPoolSize={}, connectionTimeoutMs={}, validationTimeoutMs={}, maxLifetimeMs={}",
            hikari.getPoolName(),
            hikari.getMinimumIdle(),
            hikari.getMaximumPoolSize(),
            hikari.getConnectionTimeout(),
            hikari.getValidationTimeout(),
            hikari.getMaxLifetime()
        );
    }
}
