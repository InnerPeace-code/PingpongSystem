package org.example.pingpongsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class PingpongSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(PingpongSystemApplication.class, args);
	}

}
