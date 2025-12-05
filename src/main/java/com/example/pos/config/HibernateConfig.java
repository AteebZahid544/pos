//package com.example.pos.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
//
//import javax.sql.DataSource;
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class HibernateConfig {
//
//    private final DataSource dataSource;
//
//    public HibernateConfig(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }
//
//    @Bean
//    public CurrentTenantIdentifierResolverImpl tenantIdentifierResolver() {
//        return new CurrentTenantIdentifierResolverImpl();
//    }
//
//    @Bean
//    public MultiTenantConnectionProviderImpl multiTenantConnectionProvider() {
//        return new MultiTenantConnectionProviderImpl(dataSource);
//    }
//
//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
//
//        Map<String, Object> hibernateProps = new HashMap<>();
//        hibernateProps.put("hibernate.multiTenancy", "SCHEMA");
//        hibernateProps.put("hibernate.multi_tenant_connection_provider", multiTenantConnectionProvider());
//        hibernateProps.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver());
//        hibernateProps.put("hibernate.show_sql", true);
//        hibernateProps.put("hibernate.format_sql", true);
//        hibernateProps.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect"); // important for Hibernate 6
//
//        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
//        emf.setDataSource(dataSource);
//        emf.setPackagesToScan("com.example.pos.entity.pos");
//        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
//        emf.setJpaPropertyMap(hibernateProps);
//
//        return emf;
//    }
//}
