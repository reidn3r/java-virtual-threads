package com.github.reidn3r.async_multithreading.services.interaction;

public interface InteractionStrategy<T> {
  public T handle(Long userId, Long postId);
  public boolean hasUserInteracted(Long userId, Long postId);
}
