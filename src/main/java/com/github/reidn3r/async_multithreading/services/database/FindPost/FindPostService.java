package com.github.reidn3r.async_multithreading.services.database;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostDTO;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;

import io.lettuce.core.api.sync.RedisCommands;

@Service
public class FindPostService {
  private final PostsRepository postsRepository;
  private final RedisCommands<String, String> redisCommands;

  public FindPostService(
    PostsRepository postsRepository,
    RedisCommands<String, String> redisCommands
  ) {
    this.postsRepository = postsRepository;
    this.redisCommands = redisCommands;
  }

  public InteractionPostDTO run(Long id) {
    java.util.Map<String, String> redisStats = redisCommands.hgetall("post:" + id);
    if (redisStats != null && !redisStats.isEmpty()) {
      Long likes = Long.parseLong(redisStats.getOrDefault("likes", "0"));
      Long shares = Long.parseLong(redisStats.getOrDefault("shares", "0"));
      return new InteractionPostDTO(id, likes, shares);
    }

    Optional<PostEntity> foundPost = this.postsRepository.findById(id);
    if(foundPost.isPresent()){
      return new InteractionPostDTO(
        id,
        foundPost.get().getLikes_count(),
        foundPost.get().getShares_count()
      );
    }

    return null;
  }
}
