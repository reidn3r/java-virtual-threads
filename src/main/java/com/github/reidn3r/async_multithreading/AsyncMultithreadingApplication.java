package com.github.reidn3r.async_multithreading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AsyncMultithreadingApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsyncMultithreadingApplication.class, args);
	}

}
