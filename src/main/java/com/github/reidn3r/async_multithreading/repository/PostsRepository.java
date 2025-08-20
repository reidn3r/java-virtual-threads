package com.github.reidn3r.async_multithreading.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;

import jakarta.transaction.Transactional;

@Repository
public interface PostsRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findAll();

    @Transactional
    default Optional<PostEntity> incrementLikesCount(Long postId) {
        return findById(postId).map(post -> {
            post.setLikes_count(post.getLikes_count() + 1);
            return save(post); 
        });
    }

    @Transactional
    default Optional<PostEntity> incrementSharesCount(Long postId) {
        return findById(postId).map(post -> {
            post.setShares_count(post.getShares_count() + 1);
            return save(post); 
        });
    }

    
    @Query("SELECT SUM(p.likes_count) as likeCount, SUM(p.shares_count) as shareCount FROM PostEntity p")
    InteractionPostStats stats();
}
