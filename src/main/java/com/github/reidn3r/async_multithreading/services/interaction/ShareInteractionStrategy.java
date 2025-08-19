package com.github.reidn3r.async_multithreading.services.interaction;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.domain.ShareEntity;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;
import com.github.reidn3r.async_multithreading.repository.ShareRepository;

@Service
public class ShareInteractionStrategy implements InteractionStrategy<Optional<ShareEntity>> {
  private final PostsRepository postsRepository;
  private final ShareRepository shareRepository;

  public ShareInteractionStrategy(PostsRepository postsRepository, ShareRepository sharesRepository){
    this.postsRepository = postsRepository;
    this.shareRepository = sharesRepository;
  }

  public Optional<ShareEntity> handle(Long userId, Long postId) {
    Optional<PostEntity> foundPost = this.postsRepository.findById(postId);
    boolean userHasInteracted = this.hasUserInteracted(userId, postId);

    if(!foundPost.isEmpty() && !userHasInteracted){
      this.postsRepository.incrementSharesCount(postId);

      ShareEntity shareRecord = new ShareEntity();
      shareRecord.setPosts(foundPost.get());
      shareRecord.setUserId(userId);

      this.shareRepository.save(shareRecord);
      return Optional.of(shareRecord);
    }
    return Optional.empty();
  }


  public boolean hasUserInteracted(Long userId, Long postId) {
    Optional<ShareEntity> foundInteraction = this.shareRepository.findByUserIdAndPosts_Id(userId, postId);
    return !foundInteraction.isEmpty();
  }
}
