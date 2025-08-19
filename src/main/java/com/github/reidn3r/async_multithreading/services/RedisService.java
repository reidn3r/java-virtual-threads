package com.github.reidn3r.async_multithreading.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionDTO;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionEnum;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionRedisMessage;
import com.github.reidn3r.async_multithreading.worker.Worker;

import io.lettuce.core.Consumer;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.SetArgs;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.async.RedisStreamAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PostConstruct;

@Service
public class RedisService {
  private static final String REDIS_STREAM_ID = "interactions_stream";
  private static final String FIELD_POST_ID = "postId";
  private static final String FIELD_USER_ID = "userId";
  private static final String FIELD_INTERACTION = "interaction";

  private static final String REDIS_STREAM = "interactions_stream";
  private static final String CONSUMER_GROUP = "interactions_group";
  private static final String CONSUMER_NAME = "consumer-" + UUID.randomUUID();
  
  private final RedisStreamAsyncCommands<String, String> asyncRedis;
  private final RedisCommands<String, String> redisCommands;
  private final RedisAsyncCommands<String, String> asyncCommands;
  private final Worker worker;
  
  
  public RedisService (
    RedisStreamAsyncCommands<String, String> asyncRedis,
    RedisCommands<String, String> redisCommands,
    RedisAsyncCommands<String, String> asyncCommands,
    Worker worker
  ) {
    this.redisCommands = redisCommands;
    this.asyncCommands = asyncCommands;
    this.asyncRedis = asyncRedis;
    this.worker = worker;
  }

  @PostConstruct
  public void init() {
    this.createStreamGroupIfNotExists();
  }

  public RedisFuture<String> stream(InteractionDTO data) {
    HashMap<String, String> payload = this.buildStreamPayload(data);
    return asyncRedis.xadd(REDIS_STREAM_ID, payload);
  }

  @Scheduled(fixedDelay = 100)
  public void consumeGroup(){
    RedisFuture<List<StreamMessage<String, String>>> future = this.asyncRedis.xreadgroup(
      Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
      XReadArgs.Builder.block(1),
      StreamOffset.from(REDIS_STREAM, ">")
    );

    future.thenAccept(messages -> {
      if(!messages.isEmpty()){
        for (StreamMessage<String, String> message : messages) {            
          InteractionRedisMessage msg = this.parseStreamMessage(message);

          // Verifica se a interação já existe antes de tentar salvar
          RedisFuture<String> futureExistingValue = this.findUserInteraction(msg.getUserId(), msg.getPostId(), msg.getInteractionEnum());
          futureExistingValue.thenAccept(value -> {
            if(value == null){
              this.storeUserInteaction(msg.getUserId(), msg.getPostId(), msg.getInteractionEnum());
              this.worker.process(message);
            }
          });
          this.ack(message.getId());
        }
      }
    });
  }
  
  private RedisFuture<String> storeUserInteaction(Long userId, Long postId, InteractionEnum interactionEnum){
    /*
      - K: userid:postid:interaction 
      - V: Instant.now().toString() 
      - TTL: 60s
    */

    String key = userId + "::" + postId + "::" + interactionEnum.getInteraction();
    String value = Instant.now().toString();

    SetArgs setArgs = SetArgs.Builder.ex(60); 
    return this.asyncCommands.set(key, value, setArgs);
  }
  
  private RedisFuture<String> findUserInteraction(Long userId, Long postId, InteractionEnum interactionEnum){
    String key = userId + "::" + postId + "::" + interactionEnum.getInteraction();
    return this.asyncCommands.get(key);
  }

  private InteractionRedisMessage parseStreamMessage(StreamMessage<String, String> message){
    Map<String, String> body = message.getBody();
    
    Long postId = Long.valueOf(body.get("postId"));
    Long userId = Long.valueOf(body.get("userId"));
    String interaction = body.get("interaction");
    InteractionEnum interactionEnum = InteractionEnum.valueOf(interaction);

    return new InteractionRedisMessage(postId, userId, interaction, interactionEnum);
  }

  private void ack(String messageId) {
    asyncRedis.xack(REDIS_STREAM, CONSUMER_GROUP, messageId);
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
