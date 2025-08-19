package com.github.reidn3r.async_multithreading.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.github.reidn3r.async_multithreading.domain.ShareEntity;

@Repository
public interface ShareRepository extends JpaRepository<ShareEntity, Long> {
  Optional<ShareEntity> findByUserIdAndPosts_Id(Long userId, Long postId);

}
