package com.github.reidn3r.async_multithreading.config;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.async.RedisStreamAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

  @Value("${spring.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.redis.port:6379}")
  private int redisPort;
  
  @Bean(destroyMethod = "close")
  public RedisClient redisClient() {
    String redisUrl = "redis://" + redisHost + ":" + redisPort;
    return RedisClient.create(redisUrl);
  }
    
  @Bean(destroyMethod = "close")
  public StatefulRedisConnection<String, String> redisConnection(RedisClient client) {
    return client.connect();
  }
  
  @Bean
  public RedisCommands<String, String> redisCommands(
    StatefulRedisConnection<String, String> connection) {
    return connection.sync();
  }
      
  @Bean
  public RedisStreamAsyncCommands<String, String> redisStreamAsyncCommands(
    StatefulRedisConnection<String, String> connection) {
    return connection.async();
  }

  @Bean
  public RedisAsyncCommands<String, String> redisAsyncCommands(
    StatefulRedisConnection<String, String> connection) {
    return connection.async();
  }
}