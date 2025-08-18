package com.github.reidn3r.async_multithreading.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.reidn3r.async_multithreading.services.DispatcherService;

@RestController
@RequestMapping("/interactions")
public class InteractionController {
  private DispatcherService dispatcher;

  public InteractionController(DispatcherService dispatcher){
    this.dispatcher = dispatcher;
  }

  private static final ResponseEntity<Object> CREATED_RESPONSE = 
    new ResponseEntity<Object>(HttpStatus.CREATED);

  @PostMapping()
  public ResponseEntity<Object> write(@RequestBody() String body) throws Exception {
    this.dispatcher.submit(body);
    return CREATED_RESPONSE;
  }
}
