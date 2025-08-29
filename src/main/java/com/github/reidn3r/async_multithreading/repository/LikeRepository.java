package com.github.reidn3r.async_multithreading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.github.reidn3r.async_multithreading.domain.LikeEntity;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
  // Optional<LikeEntity> findByUserIdAndPosts_Id(Long userId, Long postId);

}