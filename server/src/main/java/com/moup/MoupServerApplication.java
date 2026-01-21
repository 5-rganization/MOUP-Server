package com.moup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class MoupServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoupServerApplication.class, args);
    }
}
