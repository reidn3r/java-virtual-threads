package com.github.reidn3r.async_multithreading.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.reidn3r.async_multithreading.dto.HealthCheck.HealthCheckDTO;

@RestController
@RequestMapping("/health")
public class HealthCheckController {
  
  private final HealthCheckDTO dto;

  public HealthCheckController(HealthCheckDTO dto) {
    this.dto = dto;
  }
  
  @GetMapping()
  public ResponseEntity<HealthCheckDTO> check(){
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }
}
