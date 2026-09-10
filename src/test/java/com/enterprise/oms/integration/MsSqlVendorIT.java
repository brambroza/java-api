package com.enterprise.oms.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

@Testcontainers(disabledWithoutDocker = true)
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
