package com.github.reidn3r.async_multithreading.services;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@Service
public class DispatcherService {
  private final ObjectMapper mapper = new ObjectMapper();
  private final Validator validator;
  private final StreamService streamRedis;
  private final Executor httpExecutor;

  public DispatcherService(
    StreamService redis, 
    @Qualifier("httpExecutor") Executor httpExecutor
  ){
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    this.validator = factory.getValidator();
    this.streamRedis = redis;
    this.httpExecutor = httpExecutor;
  }

  public CompletableFuture<Void> submit(String data) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return mapper.readValue(data, InteractionDTO.class);
      } catch (Exception e) {
        throw new RuntimeException("Parse error", e);
      }
    }, httpExecutor)
    .thenComposeAsync(dto -> {
        validate(dto); // Fast validation
        return streamRedis.streamAsync(dto); // Non-blocking
    }, httpExecutor)
    .exceptionally(ex -> {
        System.err.println("Processing error: " + ex.getMessage());
        return null;
    });
  }

  private void validate(InteractionDTO dto) throws ValidationException {
    Set<ConstraintViolation<InteractionDTO>> errors = validator.validate(dto);
    
    if (!errors.isEmpty()) {
      throw new ValidationException("Erros de Validação.");
    }
  }
}
