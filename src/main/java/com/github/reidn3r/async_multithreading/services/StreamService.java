package com.github.reidn3r.async_multithreading.services;

import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionDTO;
import com.github.reidn3r.async_multithreading.services.stream.BuildStreamPayloadService;
import com.github.reidn3r.async_multithreading.services.stream.CreateConsumerGroupService;

import io.lettuce.core.api.async.RedisAsyncCommands;
import jakarta.annotation.PostConstruct;

@Service
public class StreamService {
  private static final String REDIS_STREAM = "interactions_stream";
  
  private final RedisAsyncCommands<String, String> asyncCommands;
  private final Executor redisExecutor;
  private final BuildStreamPayloadService buildStreamPayloadService;
  private final CreateConsumerGroupService createConsumerGroupService;

  public StreamService (
    RedisAsyncCommands<String, String> asyncCommands,
    Executor redisExecutor,
    BuildStreamPayloadService buildStreamPayloadService,
    CreateConsumerGroupService createConsumerGroupService
  ) {
    this.redisExecutor = redisExecutor;
    this.asyncCommands = asyncCommands;
    this.buildStreamPayloadService = buildStreamPayloadService;
    this.createConsumerGroupService = createConsumerGroupService;
  }

  @PostConstruct
  public void init() {
    this.createConsumerGroupService.run();
  }

  public void stream(InteractionDTO data) {
    redisExecutor.execute(() -> {
      try {
        var payload = buildStreamPayloadService.run(data);
        asyncCommands.xadd(REDIS_STREAM, payload)
        .exceptionally(ex -> {
          System.err.println("Redis error: " + ex.getMessage());
          return null;
        });
      } catch (Exception e) {
          System.err.println("Stream error: " + e.getMessage());
      }
    });
  }
}
