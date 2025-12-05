package com.example.pos.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.pos.repo.pos",
        entityManagerFactoryRef = "posEntityManager",
        transactionManagerRef = "posTransactionManager"
)
public class PosMultiTenantConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.pos")
    public DataSource posDataSource() {
        return new com.zaxxer.hikari.HikariDataSource();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean posEntityManager(
            MultiTenantConnectionProviderImpl connectionProvider,
            CurrentTenantIdentifierResolverImpl tenantResolver) {

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.multiTenancy", "SCHEMA");
        props.put("hibernate.multi_tenant_connection_provider", connectionProvider);
        props.put("hibernate.tenant_identifier_resolver", tenantResolver);
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        props.put("hibernate.hbm2ddl.auto", "update");

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setPackagesToScan("com.example.pos.entity.pos");
        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Primary
    @Bean(name = "posTransactionManager")
    public PlatformTransactionManager posTransactionManager(
            @Qualifier("posEntityManager") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
