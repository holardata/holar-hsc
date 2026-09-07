package com.hsc.api.config;

import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Liquibase 锁自动清理
 * 在 SpringLiquibase 执行迁移之前自动清理脏锁，
 * 解决单应用异常退出后锁未释放导致启动卡住的问题
 */
@Slf4j
@Component
public class LiquibaseLockCleaner implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SpringLiquibase) {
            clearLock((SpringLiquibase) bean);
        }
        return bean;
    }

    private void clearLock(SpringLiquibase liquibase) {
        DataSource ds = getDataSource(liquibase);
        if (ds == null) {
            return;
        }
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(
                    "update databasechangeloglock set locked=0, lockgranted=null, lockedby=null where id=1 and locked=1");
            if (rows > 0) {
                log.info("已自动清理 Liquibase 脚锁");
            }
        } catch (SQLException e) {
            log.warn("清理 Liquibase 锁失败: {}", e.getMessage());
        }
    }

    private DataSource getDataSource(SpringLiquibase liquibase) {
        try {
            Field field = SpringLiquibase.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            return (DataSource) field.get(liquibase);
        } catch (Exception e) {
            log.warn("获取 SpringLiquibase DataSource 失败: {}", e.getMessage());
            return null;
        }
    }
}
