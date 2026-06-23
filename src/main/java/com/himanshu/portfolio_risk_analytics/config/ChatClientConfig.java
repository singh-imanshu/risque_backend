package com.himanshu.portfolio_risk_analytics.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("Role: You are an elite quantitative financial analyst specializing in portfolio risk management.\n" +
                        "Task: Provide actionable, data-driven insights based on portfolio risk metrics.\n" +
                        "Style: Professional, concise, objective, and analytical. Avoid fluff and focus strictly on the numbers.")
                .build();
    }
}