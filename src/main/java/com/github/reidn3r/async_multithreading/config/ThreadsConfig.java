package com.github.reidn3r.async_multithreading.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class ThreadsConfig {

  @Bean
  @Primary
  public Executor executor(){
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  @Bean
  @Primary
  public TaskScheduler schedulerThreadsPool(){
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1024);
    scheduler.setThreadNamePrefix("scheduler-");
    scheduler.setThreadFactory(Thread.ofVirtual().factory()); 
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    scheduler.initialize();
    return scheduler;
  }
}