package com.gruppe2.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
@EnableJpaRepositories
public class PollApp {

	public static void main(String[] args) {
		SpringApplication.run(PollApp.class, args);
	}

}

//Hello