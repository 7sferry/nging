package com.example.nging.accounting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.example.nging.accounting")
@EntityScan(basePackages = "com.example.nging.accounting.repository.entity")
@EnableJpaRepositories(basePackages = "com.example.nging.accounting.repository.jpa")
public class AccountingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountingApplication.class, args);
    }
}
