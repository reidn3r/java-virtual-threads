package com.github.reidn3r.async_multithreading.dto.Interaction;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class InteractionRedisMessage {
  public Long postId;
  public Long userId;
  public String interaction;
  public InteractionEnum interactionEnum;
}
