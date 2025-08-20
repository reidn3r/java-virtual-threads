package com.github.reidn3r.async_multithreading.services;

import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionEnum;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostDTO;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;
import com.github.reidn3r.async_multithreading.services.interaction.InteractionStrategy;
import com.github.reidn3r.async_multithreading.services.interaction.LikeInteractionStrategy;
import com.github.reidn3r.async_multithreading.services.interaction.ShareInteractionStrategy;

import io.lettuce.core.api.sync.RedisCommands;

@Service
public class DbService {
  private final Map<InteractionEnum, InteractionStrategy<Optional<PostEntity>>> strategy;
  private final PostsRepository postsRepository;
  private final RedisCommands<String, String> redisCommands;

  private final JdbcTemplate jdbcTemplate;

  private static final String POSTS_HMAP = "POSTS_HMAP";
  private static final InteractionEnum LIKE = InteractionEnum.INCREMENT_LIKE;
  private static final InteractionEnum SHARE = InteractionEnum.INCREMENT_SHARE;

  public DbService(
    PostsRepository postsRepository,
    LikeInteractionStrategy likeStrategy, 
    ShareInteractionStrategy shareStrategy,
    RedisCommands<String, String> redisCommands,
    JdbcTemplate jdbcTemplate
  ){
    this.postsRepository = postsRepository;
    this.redisCommands = redisCommands;
    this.jdbcTemplate = jdbcTemplate;
    this.strategy = Map.of(
      InteractionEnum.INCREMENT_LIKE, likeStrategy,
      InteractionEnum.INCREMENT_SHARE, shareStrategy
    );
  }

  public Optional<PostEntity> save(Long userId, Long postId, String interaction){
    InteractionEnum inter = InteractionEnum.valueOf(interaction);
    InteractionStrategy<Optional<PostEntity>> strategy = this.strategy.get(inter);
    if(strategy != null){
      Optional<PostEntity> result = strategy.handle(userId, postId);
      return result;
    }
    return Optional.empty();
  }

  public InteractionPostDTO findPostById(Long id){
    String shareFfield = id + "::" + SHARE;
    String likeField = id + "::" + LIKE;
    
    String inMemoryShare = redisCommands.hget(POSTS_HMAP, shareFfield);
    String inMemoryLike = redisCommands.hget(POSTS_HMAP, likeField);

    if(inMemoryLike != null && inMemoryShare != null){
      return new InteractionPostDTO(
        id, 
        Long.valueOf(inMemoryLike), 
        Long.valueOf(inMemoryShare)
      );
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

  public InteractionPostStats stats() {
    return this.postsRepository.stats();
  }
}
