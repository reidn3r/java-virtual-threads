package com.github.reidn3r.async_multithreading.services.interaction;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionEnum;

@Component
public class InteractionFactory {
  private static Map<String, InteractionStrategy> mapper;
  
  public InteractionFactory(
    @Qualifier("likeStrategy") InteractionStrategy likeStrategy,
    @Qualifier("shareStrategy") InteractionStrategy shareStrategy
  ){
    mapper = Map.of(
      InteractionEnum.INCREMENT_LIKE.getInteraction(), likeStrategy,
      InteractionEnum.INCREMENT_SHARE.getInteraction(), shareStrategy);
  }

  public InteractionStrategy getInteractionStrategy(String interaction){
    return mapper.get(interaction);
  }
}
