package com.nirvaha.nirvaha_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NirvahaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NirvahaBackendApplication.class, args);
	}

}
