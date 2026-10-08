package com.nqd.nqd_tool_content.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = false)
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor dependsOnPostProcessor() {
        return registry -> {
            for (String beanName : registry.getBeanDefinitionNames()) {
                if (beanName.equals("entityManagerFactory")) {
                    registry.getBeanDefinition(beanName).setDependsOn("flyway");
                }
            }
        };
    }
}
