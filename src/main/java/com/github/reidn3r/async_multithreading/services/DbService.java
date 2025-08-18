package com.github.reidn3r.async_multithreading.services;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.repository.PostsRepository;

@Service
public class DbService {
  private final PostsRepository repository;

  public DbService(PostsRepository repository){
    this.repository = repository;
  }

  public void incrementLikeById(Long id){
    this.repository.incrementLikesCount(id);
  }
  
  public void incrementShareById(Long id){
    this.repository.incrementSharesCount(id);
  }
}
