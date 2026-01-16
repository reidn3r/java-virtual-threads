package com.github.reidn3r.async_multithreading.services.interaction.strategies;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionEnum;

import io.lettuce.core.api.sync.RedisCommands;

@Component
@Qualifier("shareStrategy")
class ShareInteractionStrategy implements InteractionStrategyInterface {
  private static final String PREFIX = "pending:user_share:";

  @Override
  public boolean handle(Long postId, Long userId, String interaction, RedisCommands<String, String> redisCommands) {
    if(!this.isAbleToProcess(interaction)) return false;
    
    String userShareKey = PREFIX + userId + "::" + postId;
    boolean isNewShare = redisCommands.setnx(userShareKey, Instant.now().toString());    
    if (isNewShare) {
      redisCommands.expire(userShareKey, 60*5);
    }
    return isNewShare;
  }

  @Override
  public boolean isAbleToProcess(String interactionString) {
    return interactionString.equals(InteractionEnum.INCREMENT_SHARE.toString());
  }
  
}