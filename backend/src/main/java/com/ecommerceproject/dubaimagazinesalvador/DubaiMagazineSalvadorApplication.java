package com.ecommerceproject.dubaimagazinesalvador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DubaiMagazineSalvadorApplication {

	public static void main(String[] args) {
		SpringApplication.run(DubaiMagazineSalvadorApplication.class, args);
	}

}
