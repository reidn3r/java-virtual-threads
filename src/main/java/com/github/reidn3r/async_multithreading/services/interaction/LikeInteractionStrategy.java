package com.github.reidn3r.async_multithreading.services.interaction;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.domain.LikeEntity;
import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.repository.LikeRepository;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;

@Service
public class LikeInteractionStrategy implements InteractionStrategy<Optional<PostEntity>> {
  private final PostsRepository postsRepository;
  private final LikeRepository likesRepository;

  public LikeInteractionStrategy (PostsRepository postsRepository, LikeRepository likesRepository){
    this.postsRepository = postsRepository;
    this.likesRepository = likesRepository;
  }

  public Optional<PostEntity> handle(Long userId, Long postId) {
    Optional<PostEntity> foundPost = this.postsRepository.findById(postId);
    boolean userHasInteracted = this.hasUserInteracted(userId, postId);

    if(!foundPost.isEmpty() && !userHasInteracted){
      Optional<PostEntity> postRecord = this.postsRepository.incrementLikesCount(postId);
      
      LikeEntity likeRecord = new LikeEntity();
      likeRecord.setPosts(foundPost.get());
      likeRecord.setUserId(userId);

      this.likesRepository.save(likeRecord);
      return postRecord;
    }
    return Optional.empty();
  }

  public boolean hasUserInteracted(Long userId, Long postId) {
    Optional<LikeEntity> foundInteraction = this.likesRepository.findByUserIdAndPosts_Id(userId, postId);
    return !foundInteraction.isEmpty();
  }
}
