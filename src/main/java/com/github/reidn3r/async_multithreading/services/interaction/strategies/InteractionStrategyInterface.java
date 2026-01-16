package com.github.reidn3r.async_multithreading.services.interaction.strategies;

import org.springframework.stereotype.Component;

import io.lettuce.core.api.sync.RedisCommands;

@Component
public interface InteractionStrategyInterface {
  boolean handle(Long postId, Long userId, String interaction, RedisCommands<String, String> redisCommands);
  boolean isAbleToProcess(String interactionString);
}
