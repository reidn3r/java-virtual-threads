package com.github.reidn3r.async_multithreading.controllers;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;
import com.github.reidn3r.async_multithreading.services.DispatcherService;

@RestController
@RequestMapping("/interactions")
public class InteractionController {
  private DispatcherService dispatcher;
  private PostsRepository repository;

  public InteractionController(DispatcherService dispatcher, PostsRepository repository){
    this.dispatcher = dispatcher;
    this.repository = repository;
  }

  private static final ResponseEntity<Object> CREATED_RESPONSE = 
    ResponseEntity.status(HttpStatus.CREATED).body(null);

  @PostMapping()
  public ResponseEntity<Object> write(@RequestBody() String body) throws Exception {
    this.dispatcher.submit(body);
    return CREATED_RESPONSE;
  }

  @GetMapping("/{postId}")
  public ResponseEntity<Optional<PostEntity>> postData(@PathVariable("postId") Long postId){
    Optional<PostEntity> foundPost = this.repository.findById(postId);
    return ResponseEntity.status(HttpStatus.OK).body(foundPost);
  }

  @GetMapping("/stats")
  public ResponseEntity<InteractionPostStats> stats(){
    InteractionPostStats stats = this.repository.stats();
    return ResponseEntity.status(HttpStatus.OK).body(stats);
  }
}
