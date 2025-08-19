package com.github.reidn3r.async_multithreading.services;

import java.util.Set;

import org.springframework.scheduling.annotation.Async;
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
  private final RedisService redis;

  public DispatcherService(RedisService redis){
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    this.validator = factory.getValidator();
    this.redis = redis;
  }

  @Async //TODO: Verificar Gargalo em @Async
  public void submit(String data) throws Exception {
    InteractionDTO dto = mapper.readValue(data, InteractionDTO.class);
    this.validate(dto);
    this.redis.stream(dto);
  }

  private void validate(InteractionDTO dto) throws ValidationException {
    Set<ConstraintViolation<InteractionDTO>> errors = validator.validate(dto);
    
    if (!errors.isEmpty()) {
      throw new ValidationException("Erros de Validação.");
    }
  }

}
