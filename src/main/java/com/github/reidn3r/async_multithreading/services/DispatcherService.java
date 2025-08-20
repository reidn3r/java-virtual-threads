package com.github.reidn3r.async_multithreading.services;

import java.util.Set;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
  private final Executor threadExecutor;

  public DispatcherService(StreamService redis, Executor thExecutor){
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    this.validator = factory.getValidator();
    this.streamRedis = redis;
    this.threadExecutor = thExecutor;
  }

  public void submit(String data) throws Exception {
    this.threadExecutor.execute(() -> {
      try {
        InteractionDTO dto = mapper.readValue(data, InteractionDTO.class);
        this.validate(dto);
        this.streamRedis.stream(dto);
      } catch (JsonProcessingException e) {
        System.out.println("Erro: " + e.getMessage());
      }
    });
  }

  private void validate(InteractionDTO dto) throws ValidationException {
    Set<ConstraintViolation<InteractionDTO>> errors = validator.validate(dto);
    
    if (!errors.isEmpty()) {
      throw new ValidationException("Erros de Validação.");
    }
  }
}
