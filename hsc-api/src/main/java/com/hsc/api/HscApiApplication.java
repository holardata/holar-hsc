package com.hsc.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity
@ComponentScan(basePackages = {"com.hsc"})
@MapperScan("com.hsc.**.mapper")
@EnableAsync
@SpringBootApplication
public class HscApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HscApiApplication.class, args);
    }

}
