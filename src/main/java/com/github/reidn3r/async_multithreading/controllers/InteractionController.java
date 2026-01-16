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
import com.github.reidn3r.async_multithreading.services.InteractionService;
import com.github.reidn3r.async_multithreading.services.database.DbService;

@RestController
@RequestMapping("/interactions")
public class InteractionController {
  private final DbService dbService;
  private final InteractionService interactionService;
  
  public InteractionController(
    InteractionService interactionService,
    DbService dbService
  ){
    this.interactionService = interactionService;
    this.dbService = dbService;
  }

  @PostMapping()
  public ResponseEntity<Object> write(@RequestBody() String body) {
    interactionService.run(body);
    return ResponseEntity.status(HttpStatus.CREATED).build();
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
