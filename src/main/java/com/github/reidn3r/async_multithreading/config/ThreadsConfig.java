package com.github.reidn3r.async_multithreading.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadsConfig {

    @Bean
    public Executor httpExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean 
    public Executor redisExecutor() {
        return Executors.newFixedThreadPool(4, Thread.ofVirtual().factory());
    }

    @Bean
    public Executor dbExecutor() {
        return Executors.newFixedThreadPool(8, Thread.ofVirtual().factory());
    }

    @Bean
    public Executor workerExecutor() {
        return Executors.newFixedThreadPool(2, Thread.ofVirtual().factory());
    }
}
