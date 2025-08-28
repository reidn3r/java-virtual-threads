package com.github.reidn3r.async_multithreading.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;


@Repository
public interface PostsRepository extends JpaRepository<PostEntity, Long> {
	List<PostEntity> findAll();
	
	@Query("SELECT SUM(p.likes_count) as likeCount, SUM(p.shares_count) as shareCount FROM PostEntity p")
	InteractionPostStats stats();
	
}

