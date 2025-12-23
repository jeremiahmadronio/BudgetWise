package com.example.budgetwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.data.web.config.EnableSpringDataWebSupport;
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class BudgetWiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(BudgetWiseApplication.class, args);
	}

}
