//package com.example.pos.config;
//
//import jakarta.persistence.EntityManagerFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.jdbc.DataSourceBuilder;
//import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import javax.sql.DataSource;
//
//@Configuration
//@EnableJpaRepositories(
//        basePackages = "com.example.pos.repo.central",
//        entityManagerFactoryRef = "centralEntityManager",
//        transactionManagerRef = "centralTransactionManager"
//)
//public class CentralDbConfig {
//
//    @Primary
//    @Bean
//    @ConfigurationProperties(prefix = "spring.datasource.central")
//    public DataSource centralDataSource() {
//        return DataSourceBuilder.create().build();
//    }
//
//    @Primary
//    @Bean
//    public LocalContainerEntityManagerFactoryBean centralEntityManager(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("centralDataSource") DataSource dataSource) {
//
//        return builder
//                .dataSource(dataSource) // pass the datasource here
//                .packages("com.example.pos.entity.central") // central entities
//                .persistenceUnit("central")
//                .build();
//    }
//
//    @Primary
//    @Bean
//    public PlatformTransactionManager centralTransactionManager(
//            @Qualifier("centralEntityManager") EntityManagerFactory emf) {
//        return new JpaTransactionManager(emf);
//    }
//}
