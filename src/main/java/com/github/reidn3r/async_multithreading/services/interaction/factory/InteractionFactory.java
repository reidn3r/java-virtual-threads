package com.github.reidn3r.async_multithreading.services.interaction.factory;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionEnum;
import com.github.reidn3r.async_multithreading.services.interaction.strategies.InteractionStrategyInterface;

@Component
public class InteractionFactory {
  private static Map<String, InteractionStrategyInterface> mapper;
  
  public InteractionFactory(
    @Qualifier("likeStrategy") InteractionStrategyInterface likeStrategy,
    @Qualifier("shareStrategy") InteractionStrategyInterface shareStrategy
  ){
    mapper = Map.of(
      InteractionEnum.INCREMENT_LIKE.getInteraction(), likeStrategy,
      InteractionEnum.INCREMENT_SHARE.getInteraction(), shareStrategy);
  }

  public InteractionStrategyInterface getInteractionStrategyInterface(String interaction){
    return mapper.get(interaction);
  }
}
