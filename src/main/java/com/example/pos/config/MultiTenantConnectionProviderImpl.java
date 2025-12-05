package com.example.pos.config;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
@Primary
public class MultiTenantConnectionProviderImpl
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private final DataSource defaultDataSource;
    private final Map<String, DataSource> tenantDataSources = new HashMap<>();

    public MultiTenantConnectionProviderImpl(DataSource defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
        tenantDataSources.put("default_tenant", defaultDataSource);
    }

    private DataSource createDataSource(String tenantId) {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:mysql://localhost:3306/" + tenantId) // dynamic schema
                .username("root")
                .password("0408")
                .build();
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return defaultDataSource;
    }

    @Override
    protected DataSource selectDataSource(Object tenantIdentifier) {
        String tenantId = (tenantIdentifier != null) ? tenantIdentifier.toString() : "default_tenant";
        // If tenantDataSource exists, return it. Else create dynamically
        return tenantDataSources.computeIfAbsent(tenantId, this::createDataSource);
    }

}
