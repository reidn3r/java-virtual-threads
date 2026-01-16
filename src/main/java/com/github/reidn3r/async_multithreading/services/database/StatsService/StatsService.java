package com.github.reidn3r.async_multithreading.services.database;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;

@Service
public class StatsService {
  private final PostsRepository postsRepository;

  public StatsService(PostsRepository postsRepository) {
    this.postsRepository = postsRepository;
  }

  public InteractionPostStats run() {
    return this.postsRepository.stats();
  }
}
