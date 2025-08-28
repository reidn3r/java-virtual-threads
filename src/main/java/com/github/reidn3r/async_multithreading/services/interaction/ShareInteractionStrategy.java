package com.github.reidn3r.async_multithreading.services.interaction;

import java.util.Optional;

import org.springframework.stereotype.Service;
import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.domain.ShareEntity;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;
import com.github.reidn3r.async_multithreading.repository.ShareRepository;

@Service
public class ShareInteractionStrategy implements InteractionStrategy<Optional<PostEntity>> {
  private final PostsRepository postsRepository;
  private final ShareRepository shareRepository;

  public ShareInteractionStrategy(PostsRepository postsRepository, ShareRepository sharesRepository){
    this.postsRepository = postsRepository;
    this.shareRepository = sharesRepository;
  }

  public Optional<PostEntity> handle(Long userId, Long postId) {
    boolean userHasInteracted = this.hasUserInteracted(userId, postId);
  //   if(!userHasInteracted){
  //     this.postsRepository.incrementSharesCount(postId);

  //     ShareEntity shareRecord = new ShareEntity();
  //     shareRecord.setUserId(userId);

  //     this.shareRepository.save(shareRecord);
  //     return postsRepository.findById(postId);
  //   }
    return Optional.empty();
  }

  public boolean hasUserInteracted(Long userId, Long postId) {
    Optional<ShareEntity> foundInteraction = this.shareRepository.findByUserIdAndPosts_Id(userId, postId);
    return !foundInteraction.isEmpty();
  }
}
