package com.github.reidn3r.async_multithreading.services;

import java.util.HashMap;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionDTO;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PostConstruct;

@Service
public class StreamService {
  private static final String FIELD_POST_ID = "postId";
  private static final String FIELD_USER_ID = "userId";
  private static final String FIELD_INTERACTION = "interaction";

  private static final String REDIS_STREAM = "interactions_stream";
  private static final String CONSUMER_GROUP = "interactions_group";
  
  private final RedisCommands<String, String> redisCommands;
  private final RedisAsyncCommands<String, String> asyncCommands;
  private final Executor redisExecutor;

  public StreamService (
    RedisAsyncCommands<String, String> asyncCommands,
    RedisCommands<String, String> redisCommands,
    Executor redisExecutor
  ) {
    this.redisExecutor = redisExecutor;
    this.redisCommands = redisCommands;
    this.asyncCommands = asyncCommands;
  }

  @PostConstruct
  public void init() {
    this.createStreamGroupIfNotExists();
  }

  public void stream(InteractionDTO data) {
    redisExecutor.execute(() -> {
      try {
        HashMap<String, String> payload = buildStreamPayload(data);
        asyncCommands.xadd("interactions_stream", payload)
        .exceptionally(ex -> {
          System.err.println("Redis error: " + ex.getMessage());
          return null;
        });
      } catch (Exception e) {
          System.err.println("Stream error: " + e.getMessage());
      }
    });
  }

  private HashMap<String, String> buildStreamPayload(InteractionDTO data){
    HashMap<String, String> payload = new HashMap<String, String>();
    String postId = Integer.toString(data.postId());
    String userId= Integer.toString(data.userId());

    payload.put(FIELD_POST_ID, postId);
    payload.put(FIELD_USER_ID, userId);
    payload.put(FIELD_INTERACTION, data.interaction().getInteraction());
    return payload;
  }

  private void createStreamGroupIfNotExists() {
    try {
      redisCommands.xgroupCreate(
        XReadArgs.StreamOffset.from(REDIS_STREAM, "0-0"), 
        CONSUMER_GROUP,
        XGroupCreateArgs.Builder.mkstream(true) // Cria o stream se não existir
      );
      } catch (RedisBusyException e) {
        System.out.println("Consumer group já existente - continuando normalmente");
      } catch (Exception e) {
        System.out.println("Falha crítica na inicialização do Redis: " + e);
        throw new RuntimeException("Não foi possível inicializar o RedisService", e);
    }
  }
}
