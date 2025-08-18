package com.github.reidn3r.async_multithreading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisStreamAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;

@Configuration
public class RedisConfig {
  
  @Bean(destroyMethod = "close")
  public RedisClient redisClient() {
    return RedisClient.create("redis://localhost");
  }
    
  @Bean(destroyMethod = "close")
  public StatefulRedisConnection<String, String> redisConnection(RedisClient client) {
    return client.connect();
  }
  
  @Bean
  public RedisCommands<String, String> syncRedis(
    StatefulRedisConnection<String, String> connection) {
    return connection.sync();
  }
      
  @Bean
  public RedisStreamAsyncCommands<String, String> asyncRedis(
    StatefulRedisConnection<String, String> connection) {
    return connection.async();
  }
}