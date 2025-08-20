package com.github.reidn3r.async_multithreading.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostDTO;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;
import com.github.reidn3r.async_multithreading.services.DbService;
import com.github.reidn3r.async_multithreading.services.DispatcherService;

@RestController
@RequestMapping("/interactions")
public class InteractionController {
  private DispatcherService dispatcher;
  private DbService dbService;
  
  public InteractionController(
    DispatcherService dispatcher,
    DbService dbService
  ){
    this.dispatcher = dispatcher;
    this.dbService = dbService;
  }

  private static final ResponseEntity<Object> CREATED_RESPONSE = 
    ResponseEntity.status(HttpStatus.CREATED).build();

  @PostMapping()
  public ResponseEntity<Object> write(@RequestBody() String body) throws Exception {
    this.dispatcher.submit(body);
    return CREATED_RESPONSE;
  }

  @GetMapping("/{postId}")
  public ResponseEntity<InteractionPostDTO> postData(@PathVariable("postId") Long postId){
    InteractionPostDTO foundPost = this.dbService.findPostById(postId);
    return ResponseEntity.status(HttpStatus.OK).body(foundPost);
  }

  @GetMapping("/stats")
  public ResponseEntity<InteractionPostStats> stats(){
    InteractionPostStats stats = this.dbService.stats();
    return ResponseEntity.status(HttpStatus.OK).body(stats);
  }
}
