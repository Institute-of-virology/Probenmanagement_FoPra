package de.unimarburg.samplemanagement.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Configuration
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FlywayInitializer.class);

    private final DataSource dataSource;

    public FlywayInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Flyway migration...");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        logger.info("Flyway migration completed successfully.");
    }
}