package org.enterpriseauditing.enterpriseauditing;

import org.springframework.boot.SpringApplication;

public class TestEnterpriseAuditingApplication {

    public static void main(String[] args) {
        SpringApplication.from(EnterpriseAuditingApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
