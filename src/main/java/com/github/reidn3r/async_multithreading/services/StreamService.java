package com.github.reidn3r.async_multithreading.services;

import java.util.HashMap;
import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionDTO;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.async.RedisStreamAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PostConstruct;

@Service
public class StreamService {
  private static final String REDIS_STREAM_ID = "interactions_stream";
  private static final String FIELD_POST_ID = "postId";
  private static final String FIELD_USER_ID = "userId";
  private static final String FIELD_INTERACTION = "interaction";

  private static final String REDIS_STREAM = "interactions_stream";
  private static final String CONSUMER_GROUP = "interactions_group";
  
  private final RedisStreamAsyncCommands<String, String> asyncRedis;
  private final RedisCommands<String, String> redisCommands;
    
  public StreamService (
    RedisStreamAsyncCommands<String, String> asyncRedis,
    RedisCommands<String, String> redisCommands
  ) {
    this.redisCommands = redisCommands;
    this.asyncRedis = asyncRedis;
  }

  @PostConstruct
  public void init() {
    this.createStreamGroupIfNotExists();
  }

  public RedisFuture<String> stream(InteractionDTO data) {
    HashMap<String, String> payload = this.buildStreamPayload(data);
    return asyncRedis.xadd(REDIS_STREAM_ID, payload);
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
