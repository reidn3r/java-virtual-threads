package com.github.reidn3r.async_multithreading.services.stream;

import org.springframework.stereotype.Service;

import io.lettuce.core.RedisBusyException;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.sync.RedisCommands;

@Service
public class CreateConsumerGroupService {
  private static final String REDIS_STREAM = "interactions_stream";
  private static final String CONSUMER_GROUP = "interactions_group";
  private final RedisCommands<String, String> redisCommands;

  public CreateConsumerGroupService(RedisCommands<String, String> redisCommands) {
    this.redisCommands = redisCommands;
  }

  public void run() {
    try {
      redisCommands.xgroupCreate(
        XReadArgs.StreamOffset.from(REDIS_STREAM, "0-0"), 
        CONSUMER_GROUP,
        XGroupCreateArgs.Builder.mkstream(true)
      );
    } catch (RedisBusyException e) {
      System.out.println("Consumer group já existente - continuando normalmente");
    } catch (Exception e) {
      System.out.println("Falha crítica na inicialização do Redis: " + e);
      throw new RuntimeException("Não foi possível inicializar o RedisService", e);
    }
  }
}
