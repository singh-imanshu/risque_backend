package com.himanshu.portfolio_risk_analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PortfolioRiskAnalyticsApplication {

	public static void main(String[] args) {
	    String mongoUri = System.getenv("MONGO_DB_URI");
	    System.out.println("DEBUG: MONGO_DB_URI is " 
	        + (mongoUri == null ? "NULL" : "present, length=" + mongoUri.length() 
	        + ", starts with: " + mongoUri.substring(0, Math.min(15, mongoUri.length()))));
	    SpringApplication.run(PortfolioRiskAnalyticsApplication.class, args);
	}
}
