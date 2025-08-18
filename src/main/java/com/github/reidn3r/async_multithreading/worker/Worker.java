package com.github.reidn3r.async_multithreading.worker;

import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Component;
import io.lettuce.core.StreamMessage;

@Component
public class Worker {
  private final Executor threadExecutor;

  public Worker(Executor threadExecutor){
    this.threadExecutor = threadExecutor;
  }

  public void process(StreamMessage<String, String> message){
    threadExecutor.execute(() -> {
      Map<String, String> body = message.getBody();
                
      Long postId = Long.valueOf(body.get("postId"));
      Long userId = Long.valueOf(body.get("userId"));
      String interaction = body.get("interaction");

      System.out.println("Virtual Thread: OK!");
      
      // Processamento em virtual thread
    });
  }
  }
