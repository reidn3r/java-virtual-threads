package com.github.reidn3r.async_multithreading.services;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionEnum;
import com.github.reidn3r.async_multithreading.services.interaction.InteractionStrategy;
import com.github.reidn3r.async_multithreading.services.interaction.LikeInteractionStrategy;
import com.github.reidn3r.async_multithreading.services.interaction.ShareInteractionStrategy;

@Service
public class DbService {
  private final Map<InteractionEnum, InteractionStrategy> strategy;

  public DbService(LikeInteractionStrategy likeStrategy, ShareInteractionStrategy shareStrategy){
    this.strategy = Map.of(
      InteractionEnum.INCREMENT_LIKE, likeStrategy,
      InteractionEnum.INCREMENT_SHARE, shareStrategy
    );
  }

  public void save(Long userId, Long postId, String interaction){
    InteractionEnum inter = InteractionEnum.valueOf(interaction);
    InteractionStrategy strategy = this.strategy.get(inter);
    if(strategy != null){
      strategy.handle(userId, postId);
    }
  }
}
