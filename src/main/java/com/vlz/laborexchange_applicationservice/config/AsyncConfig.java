package com.vlz.laborexchange_applicationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean(name = "notificationExecutor", destroyMethod = "shutdown")
    public ExecutorService notificationExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
