package com.enterprise.oms.shared.infrastructure.persistence;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.stereotype.Component;

/**
 * Detects the database vendor behind the DataSource at startup, logs it, and exposes it on
 * {@code /actuator/info} so operators can verify which vendor/dialect a running instance uses.
 */
@Component
public class DatabaseVendorInfo implements InfoContributor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseVendorInfo.class);

    private final String vendor;
    private final String productName;
    private final String productVersion;
    private final String driver;
    private final String dialect;

    public DatabaseVendorInfo(DataSource dataSource, EntityManagerFactory entityManagerFactory) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            this.productName = metaData.getDatabaseProductName();
            this.productVersion = metaData.getDatabaseProductVersion();
            this.driver = metaData.getDriverName() + " " + metaData.getDriverVersion();
            this.vendor = DatabaseDriver.fromProductName(productName).getId();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read database metadata", e);
        }
        this.dialect = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getJdbcServices().getDialect().getClass().getSimpleName();
        log.info("Database vendor={} product='{}' version='{}' dialect={} driver='{}'",
                vendor, productName, productVersion, dialect, driver);
    }

    public String vendor() {
        return vendor;
    }

    public String dialect() {
        return dialect;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("vendor", vendor);
        details.put("product", productName);
        details.put("version", productVersion);
        details.put("dialect", dialect);
        details.put("driver", driver);
        builder.withDetail("database", details);
    }
}
