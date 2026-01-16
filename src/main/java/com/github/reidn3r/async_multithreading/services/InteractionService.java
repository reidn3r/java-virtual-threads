package com.github.reidn3r.async_multithreading.services;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionDTO;

@Service
public class InteractionService {
  private final Executor httpExecutor;
  private final StreamService streamService;
  private final ObjectMapper mapper;

  public InteractionService(
    StreamService streamService,
    @Qualifier("httpExecutor") Executor httpExecutor
  ) {
    this.streamService = streamService;
    this.httpExecutor = httpExecutor;
    this.mapper = new ObjectMapper();
  }

  public void run(String body) {
    httpExecutor.execute(() -> {
      try {
        InteractionDTO dto = mapper.readValue(body, InteractionDTO.class);
        streamService.stream(dto);
      } catch (Exception e) {
        System.err.println("Error processing: " + e.getMessage());
      }
    });
  }
}
