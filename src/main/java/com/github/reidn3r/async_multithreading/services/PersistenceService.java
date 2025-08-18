package com.github.reidn3r.async_multithreading.services;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.repository.PostsRepository;

@Service
public class PersistenceService {
  private final PostsRepository repository;

  public PersistenceService(PostsRepository repository){
    this.repository = repository;
  }

  public void incrementLikeById(Long id){
    this.repository.incrementLikesCount(id);
  }
  
  // public void incrementShareById(Long id){
  //   this.repository.incrementSharesCountByPostId(id);
  // }
}
