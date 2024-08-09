package com.ksyun.timeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TimeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeServiceApplication.class, args);
    }

}
