package com.himanshu.portfolio_risk_analytics.controller;

import com.himanshu.portfolio_risk_analytics.dto.ApiResponse;
import com.himanshu.portfolio_risk_analytics.dto.AnalysisRequest;
import com.himanshu.portfolio_risk_analytics.dto.AnalysisResponse;
import com.himanshu.portfolio_risk_analytics.dto.RiskMetricsDto;
import com.himanshu.portfolio_risk_analytics.service.RiskService;
import com.himanshu.portfolio_risk_analytics.service.StockDataService;
import com.himanshu.portfolio_risk_analytics.repository.PortfolioRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "*")

public class AnalyticsController {
    private static final Logger logger = Logger.getLogger(AnalyticsController.class.getName());
    private final RiskService riskService;
    private final StockDataService stockDataService;
    private final ChatClient chatClient;
    private final PortfolioRepository portfolioRepository;

    public AnalyticsController(RiskService riskService,
                               StockDataService stockDataService,
                               ChatClient chatClient,
                               PortfolioRepository portfolioRepository) {
        this.riskService = riskService;
        this.stockDataService = stockDataService;
        this.chatClient = chatClient;
        this.portfolioRepository = portfolioRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyzePortfolio(
            @Valid @RequestBody AnalysisRequest request,
            Authentication authentication) {
        try {
            logger.log(Level.INFO, "Portfolio analysis started for: " + request.getTickers());

            // Validate input
            if (request.getTickers() == null || request.getTickers().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Tickers list cannot be empty", "INVALID_INPUT"));
            }

            // Use equal weights if not provided
            double[] weights = request.getWeights() != null && !request.getWeights().isEmpty()
                    ? request.getWeights().stream().mapToDouble(Double::doubleValue).toArray()
                    : createEqualWeights(request.getTickers().size());

            // Validate weights sum to ~1.0
            double weightSum = Arrays.stream(weights).sum();
            if (Math.abs(weightSum - 1.0) > 0.01) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Weights must sum to 1.0 (current sum: " + String.format("%.4f", weightSum) + ")", "INVALID_WEIGHTS"));
            }

            // Calculate risk metrics
            RiskMetricsDto riskMetrics = riskService.calculateRiskMetrics(
                    request.getTickers(), weights);

            // Generate AI insights ONLY if analysisType is COMPREHENSIVE
            String aiInsight = null;
            if ("COMPREHENSIVE".equalsIgnoreCase(request.getAnalysisType())) {
                aiInsight = generateAIInsight(riskMetrics);
            }

            // Update portfolio if portfolioId is provided
            if (request.getPortfolioId() != null && !request.getPortfolioId().isEmpty()) {
                portfolioRepository.findByIdAndUserId(request.getPortfolioId(), authentication.getName())
                        .ifPresent(portfolio -> {
                            portfolio.setTotalVolatility(riskMetrics.getVolatility());
                            portfolio.setExpectedReturn(riskMetrics.getExpectedReturn());
                            portfolio.setLastAnalyzedAt(LocalDateTime.now());
                            portfolioRepository.save(portfolio);
                        });
            }

            AnalysisResponse response = AnalysisResponse.builder()
                    .riskMetrics(riskMetrics)
                    .aiInsight(aiInsight)
                    .analyzedAt(LocalDateTime.now())
                    .analysisType(request.getAnalysisType())
                    .build();

            logger.log(Level.INFO, "Portfolio analysis completed successfully");
            return ResponseEntity.ok(ApiResponse.success(response, "Analysis complete"));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Analysis failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Analysis failed: " + e.getMessage(), "ANALYSIS_FAILED"));
        }
    }

    @PostMapping("/quick-analysis")
    public ResponseEntity<ApiResponse<RiskMetricsDto>> quickAnalysis(
            @Valid @RequestBody AnalysisRequest request) {
        try {
            if (request.getTickers() == null || request.getTickers().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Tickers list cannot be empty", "INVALID_INPUT"));
            }

            double[] weights = request.getWeights() != null && !request.getWeights().isEmpty()
                    ? request.getWeights().stream().mapToDouble(Double::doubleValue).toArray()
                    : createEqualWeights(request.getTickers().size());

            // Validate weights sum to ~1.0
            double weightSum = Arrays.stream(weights).sum();
            if (Math.abs(weightSum - 1.0) > 0.01) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Weights must sum to 1.0 (current sum: " + String.format("%.4f", weightSum) + ")", "INVALID_WEIGHTS"));
            }

            RiskMetricsDto metrics = riskService.calculateRiskMetrics(request.getTickers(), weights);
            return ResponseEntity.ok(ApiResponse.success(metrics, "Quick analysis complete"));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Quick analysis failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage(), "QUICK_ANALYSIS_FAILED"));
        }
    }

    private double[] createEqualWeights(int size) {
        double[] weights = new double[size];
        double weight = 1.0 / size;
        for (int i = 0; i < size; i++) {
            weights[i] = weight;
        }
        return weights;
    }

    private String generateAIInsight(RiskMetricsDto metrics) {
        try {
            String prompt = String.format(
                    "Please analyze the following portfolio risk metrics and provide a concise investment recommendation.\n\n" +
                            "### Portfolio Metrics ###\n" +
                            "- Tickers: %s\n" +
                            "- Expected Annual Return: %.2f%%\n" +
                            "- Volatility: %.2f%%\n" +
                            "- Sharpe Ratio: %.2f\n" +
                            "- Beta (Market Risk): %.2f\n" +
                            "- CVaR (95%%): %.2f%%\n" +
                            "- Max Drawdown: %.2f%%\n\n" +
                            "### Instructions ###\n" +
                            "1. Briefly assess the risk-adjusted performance (using Sharpe Ratio and Return vs. Volatility).\n" +
                            "2. Evaluate the downside risk (using CVaR and Max Drawdown).\n" +
                            "3. Provide a clear, actionable investment recommendation (e.g., rebalance, hold, reduce risk).\n" +
                            "4. Keep the entire response strictly between 2 to 4 sentences.",
                    String.join(", ", metrics.getTickers()),
                    metrics.getExpectedReturn() * 100,
                    metrics.getVolatility() * 100,
                    metrics.getSharpeRatio(),
                    metrics.getBeta(),
                    metrics.getConditionalVar95() * 100,
                    metrics.getMaxDrawdown() * 100
            );

            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            logger.log(Level.WARNING, "AI insight generation failed.", e);
            return "Unable to generate AI insights at this time. Please review the risk metrics above.";
        }
    }
}