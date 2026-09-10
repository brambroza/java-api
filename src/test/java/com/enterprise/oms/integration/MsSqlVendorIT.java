package com.enterprise.oms.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

@Testcontainers(disabledWithoutDocker = true)
// mirrors application-mssql.yml: SQL Server needs NVARCHAR for Unicode text
@TestPropertySource(properties = "spring.jpa.properties.hibernate.use_nationalized_character_data=true")
class MsSqlVendorIT extends AbstractVendorContractIT {

    @Container
    @ServiceConnection
    static final MSSQLServerContainer MSSQL =
            new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Override
    protected String expectedVendor() {
        return "sqlserver";
    }
}
