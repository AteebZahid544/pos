package com.example.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication(
		exclude = {
				org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
				org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
				org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class
		}
)
public class PosApplication {

	public static void main(String[] args) {
		SpringApplication.run(PosApplication.class, args);
	}
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}


}
