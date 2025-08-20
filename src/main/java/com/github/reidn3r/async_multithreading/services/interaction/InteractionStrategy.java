package com.github.reidn3r.async_multithreading.services.interaction;

public interface InteractionStrategy<PostEntity> {
  public PostEntity handle(Long userId, Long postId);
  public boolean hasUserInteracted(Long userId, Long postId);
}
