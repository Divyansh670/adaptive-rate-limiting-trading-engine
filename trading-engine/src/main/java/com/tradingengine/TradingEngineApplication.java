package com.tradingengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TradingEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingEngineApplication.class, args);
	}

}
