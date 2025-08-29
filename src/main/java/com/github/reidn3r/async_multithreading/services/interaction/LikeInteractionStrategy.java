package com.github.reidn3r.async_multithreading.services.interaction;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionEnum;

import io.lettuce.core.api.sync.RedisCommands;

@Component
@Qualifier("likeStrategy")
class LikeInteractionStrategy implements InteractionStrategy {
	private static final String PREFIX = "pending:user_like:";

  public boolean isAbleToProcess(String interactioString) {
    return interactioString.equals(InteractionEnum.INCREMENT_LIKE.getInteraction());
  }

  public boolean handle(Long postId, Long userId, String interaction, RedisCommands<String, String> redisCommands) {
    if(!this.isAbleToProcess(interaction)) return false;
    
    String userLikeKey = PREFIX + userId + "::" + postId;
    boolean isNewLike = redisCommands.setnx(userLikeKey, Instant.now().toString());    
    if (isNewLike) {
      redisCommands.expire(userLikeKey, 60*5);
    }
    return isNewLike;
  }
}