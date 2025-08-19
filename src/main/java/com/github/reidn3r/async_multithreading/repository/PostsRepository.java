package com.github.reidn3r.async_multithreading.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.github.reidn3r.async_multithreading.domain.PostEntity;

import jakarta.transaction.Transactional;

@Repository
public interface PostsRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findAll();

    @Modifying
    @Transactional
    @Query("UPDATE PostEntity p SET p.likes_count = p.likes_count + 1 WHERE p.id = :postId")
    void incrementLikesCount(@Param("postId") Long postId);
    
    @Modifying
    @Transactional
    @Query("UPDATE PostEntity p SET p.shares_count = p.shares_count + 1 WHERE p.id = :postId")
    void incrementSharesCount(@Param("postId") Long postId);
}
