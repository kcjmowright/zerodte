package com.kcjmowright.zerodte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZeroDTEAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZeroDTEAgentApplication.class, args);
	}
}
