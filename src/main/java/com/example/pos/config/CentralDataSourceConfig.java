package com.example.pos.config;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
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

import java.util.HashMap;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.pos.repo.central",
        entityManagerFactoryRef = "centralEntityManager",
        transactionManagerRef = "centralTransactionManager"
)
public class CentralDataSourceConfig {

    @Primary
    @Bean(name = "centralDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.central")
    public DataSource centralDataSource() {
        return new com.zaxxer.hikari.HikariDataSource();
    }


    @Bean(name = "centralEntityManager")
    public LocalContainerEntityManagerFactoryBean centralEntityManager(@Qualifier("centralDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.pos.entity.central");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "centralTransactionManager")
    public PlatformTransactionManager centralTransactionManager(@Qualifier("centralEntityManager") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
