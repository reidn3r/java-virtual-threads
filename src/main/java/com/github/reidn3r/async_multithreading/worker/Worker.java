package com.github.reidn3r.async_multithreading.worker;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionEnum;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionRedisMessage;
import com.github.reidn3r.async_multithreading.services.DbService;

import io.lettuce.core.Consumer;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.SetArgs;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.async.RedisStreamAsyncCommands;

@Component
public class Worker {
  private static final String REDIS_STREAM = "interactions_stream";
  private static final String CONSUMER_GROUP = "interactions_group";
  private static final String CONSUMER_NAME = "consumer-" + UUID.randomUUID();

  private static final String POSTS_HMAP = "POSTS_HMAP";
  private static final InteractionEnum LIKE = InteractionEnum.INCREMENT_LIKE;
  private static final InteractionEnum SHARE = InteractionEnum.INCREMENT_SHARE;


  private final Executor threadExecutor;
  private final DbService pgsql;
  private final RedisAsyncCommands<String, String> asyncCommands;
  private final RedisStreamAsyncCommands<String, String> asyncRedis;

  public Worker(
    Executor threadExecutor, 
    DbService db,
    RedisAsyncCommands<String, String> asyncCommands,
    RedisStreamAsyncCommands<String, String> asyncRedis
  ) {
    this.threadExecutor = threadExecutor;
    this.pgsql = db;
    this.asyncCommands = asyncCommands;
    this.asyncRedis = asyncRedis;
  }

  public void process(StreamMessage<String, String> message){
    threadExecutor.execute(() -> {
      Map<String, String> body = message.getBody();
                
      Long postId = Long.valueOf(body.get("postId"));
      Long userId = Long.valueOf(body.get("userId"));
      String interaction = body.get("interaction");

      Optional<PostEntity> result = pgsql.save(userId, postId, interaction);
      if(result.isPresent()){
        this.updatePostStatus(postId, result.get());
      }
    });
  }

  @Scheduled(fixedDelay = 10)
  public void consume(){
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
          this.storeUserInteaction(msg.getUserId(), msg.getPostId(), msg.getInteractionEnum());
          futureExistingValue.thenAccept(value -> {
            if(value == null){
              this.process(message);
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

  private RedisFuture<Boolean> updatePostStatus(Long postId, PostEntity updatedPost){
    /*
     * HashMap
     * K: Hash Name - posts
     * Map<String, String> - pares (chave, valor) dentro do HashMap
     * 
     * posts_hmap { postId:like: 1, postId:share: 0 }
     */
    String shareField = postId + "::" + SHARE;
    Long newLikeAmount = updatedPost.getLikes_count();
    
    String likeField = postId + "::" + LIKE;
    Long newShareAmount = updatedPost.getShares_count();

    this.asyncCommands.hset(POSTS_HMAP, shareField, newShareAmount.toString());
    return this.asyncCommands.hset(POSTS_HMAP, likeField, newLikeAmount.toString());
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
}
