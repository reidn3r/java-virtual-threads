package com.github.reidn3r.async_multithreading.controllers;

import java.util.concurrent.Executor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionDTO;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostDTO;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;
import com.github.reidn3r.async_multithreading.services.DbService;
import com.github.reidn3r.async_multithreading.services.StreamService;

@RestController
@RequestMapping("/interactions")
public class InteractionController {
  private DbService dbService;
  private final Executor httpExecutor;
  private final StreamService streamService;
  private final ObjectMapper mapper = new ObjectMapper();
  
  public InteractionController(
    StreamService streamService,
    DbService dbService,
    Executor httpExecutor
  ){
    this.streamService = streamService;
    this.dbService = dbService;
    this.httpExecutor = httpExecutor;
  }

  @PostMapping()
    public ResponseEntity<Object> write(@RequestBody() String body) {
        // Processamento ASSÍNCRONO REAL - não espera resultado
        httpExecutor.execute(() -> {
            try {
                InteractionDTO dto = mapper.readValue(body, InteractionDTO.class);
                streamService.streamFireAndForget(dto); // NÃO BLOQUEANTE
            } catch (Exception e) {
                System.err.println("Error processing: " + e.getMessage());
            }
        });
        
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
