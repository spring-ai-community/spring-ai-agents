/*
 * Copyright 2024 Spring AI Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.agents.claude.sdk.integration;

import org.springaicommunity.agents.claude.sdk.Query;
import org.springaicommunity.agents.claude.sdk.ClaudeAgentClient;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests that validate domain object calculations and behavior
 * with real Claude CLI responses.
 *
 * <p>
 * These tests are critical for ensuring our domain objects work correctly and our
 * documentation is accurate.
 * </p>
 *
 * <p>
 * This test extends {@link ClaudeCliTestBase} which automatically discovers Claude CLI
 * and ensures all tests fail gracefully with a clear message if Claude CLI is not
 * available.
 * </p>
 */
class DomainObjectIT extends ClaudeCliTestBase {

	private static final Logger logger = LoggerFactory.getLogger(DomainObjectIT.class);

	@Test
	void testCostCalculationsWithRealData() throws Exception {
		logger.info("Testing Cost calculations with real Claude CLI response");

		// Execute a simple query that should have measurable cost
		CLIOptions options = CLIOptions.builder()
			.timeout(Duration.ofMinutes(3)) // AgentClient overhead requires longer
											// timeout
			.build();

		QueryResult result = Query.execute("What is 2+2? Please explain your reasoning.", options);

		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();

		Cost cost = result.metadata().cost();
		assertThat(cost).isNotNull();

		// Validate cost structure
		assertThat(cost.inputTokenCost()).isGreaterThanOrEqualTo(0.0);
		assertThat(cost.outputTokenCost()).isGreaterThanOrEqualTo(0.0);
		assertThat(cost.inputTokens()).isGreaterThan(0);
		assertThat(cost.outputTokens()).isGreaterThan(0);
		assertThat(cost.model()).isNotNull();

		// Test calculation methods
		double total = cost.calculateTotal();
		assertThat(total).isEqualTo(cost.inputTokenCost() + cost.outputTokenCost());

		double withMarkup = cost.calculateWithMarkup(0.15);
		assertThat(withMarkup).isEqualTo(total * 1.15);

		double perToken = cost.calculatePerToken();
		int totalTokens = cost.inputTokens() + cost.outputTokens();
		assertThat(perToken).isEqualTo(total / totalTokens);

		// Test rate calculations
		double inputRate = cost.getInputCostPerThousandTokens();
		double expectedInputRate = (cost.inputTokenCost() / cost.inputTokens()) * 1000;
		assertThat(inputRate).isCloseTo(expectedInputRate, within(0.000001));

		double outputRate = cost.getOutputCostPerThousandTokens();
		double expectedOutputRate = (cost.outputTokenCost() / cost.outputTokens()) * 1000;
		assertThat(outputRate).isCloseTo(expectedOutputRate, within(0.000001));

		// Test business logic
		boolean expensive = cost.isExpensive();
		assertThat(expensive).isEqualTo(total > 0.10);

		double efficiency = cost.getEfficiencyRatio();
		assertThat(efficiency).isEqualTo(cost.outputTokens() / total);

		logger.info("Cost validation passed - Total: ${}, Tokens: {}, Efficiency: {}", total, totalTokens, efficiency);
	}

	@Test
	void testUsageAnalyticsWithRealData() throws Exception {
		logger.info("Testing Usage analytics with real Claude CLI response");

		// Execute a query that should generate thinking tokens
		CLIOptions options = CLIOptions.builder().maxTokens(500).systemPrompt("Think step by step").build();

		QueryResult result = Query.execute(
				"Solve this logic puzzle: If A is taller than B, and B is taller than C, who is the shortest?",
				options);

		assertThat(result).isNotNull();
		Usage usage = result.metadata().usage();
		assertThat(usage).isNotNull();

		// Validate usage structure
		assertThat(usage.inputTokens()).isGreaterThan(0);
		assertThat(usage.outputTokens()).isGreaterThan(0);
		assertThat(usage.thinkingTokens()).isGreaterThanOrEqualTo(0);

		// Test calculation methods
		int totalTokens = usage.getTotalTokens();
		assertThat(totalTokens).isEqualTo(usage.inputTokens() + usage.outputTokens() + usage.thinkingTokens());

		double compressionRatio = usage.getCompressionRatio();
		double expectedCompression = (double) usage.outputTokens() / usage.inputTokens();
		assertThat(compressionRatio).isCloseTo(expectedCompression, within(0.000001));

		double thinkingRatio = usage.getThinkingRatio();
		double expectedThinking = (double) usage.thinkingTokens() / totalTokens;
		assertThat(thinkingRatio).isCloseTo(expectedThinking, within(0.000001));

		double inputPercentage = usage.getInputTokenPercentage();
		double expectedInputPercentage = (double) usage.inputTokens() / totalTokens * 100;
		assertThat(inputPercentage).isCloseTo(expectedInputPercentage, within(0.000001));

		double outputPercentage = usage.getOutputTokenPercentage();
		double expectedOutputPercentage = (double) usage.outputTokens() / totalTokens * 100;
		assertThat(outputPercentage).isCloseTo(expectedOutputPercentage, within(0.000001));

		// Test business logic
		boolean exceedsLimit = usage.exceedsLimit(1000);
		assertThat(exceedsLimit).isEqualTo(totalTokens > 1000);

		boolean largeResponse = usage.isLargeResponse();
		assertThat(largeResponse).isEqualTo(totalTokens > 5000);

		boolean significantThinking = usage.hasSignificantThinking();
		assertThat(significantThinking).isEqualTo(thinkingRatio > 0.1);

		logger.info("Usage validation passed - Total: {}, Input: {}, Output: {}, Thinking: {}, Compression: {}",
				totalTokens, usage.inputTokens(), usage.outputTokens(), usage.thinkingTokens(), compressionRatio);
	}

	@Test
	void testMetadataAggregationWithRealData() throws Exception {
		logger.info("Testing Metadata aggregation with real Claude CLI response");

		long startTime = System.currentTimeMillis();

		CLIOptions options = CLIOptions.builder()
			.timeout(Duration.ofMinutes(3)) // AgentClient overhead requires longer
											// timeout
			.build();

		QueryResult result = Query.execute("Write a haiku about programming", options);

		long endTime = System.currentTimeMillis();
		long measuredDuration = endTime - startTime;

		assertThat(result).isNotNull();
		Metadata metadata = result.metadata();
		assertThat(metadata).isNotNull();

		// Validate metadata structure
		assertThat(metadata.model()).isNotNull();
		assertThat(metadata.getDuration()).isNotNull();
		assertThat(metadata.cost()).isNotNull();
		assertThat(metadata.usage()).isNotNull();
		assertThat(metadata.sessionId()).isNotNull();

		// Test duration accuracy (should be reasonably close to measured)
		long reportedDuration = metadata.getDuration().toMillis();
		assertThat(reportedDuration).isLessThanOrEqualTo(measuredDuration + 1000); // Allow
																					// 1s
																					// tolerance

		// Test efficiency calculations
		double efficiency = metadata.getEfficiencyScore();
		double expectedEfficiency = metadata.usage().getTotalTokens() / (double) reportedDuration;
		assertThat(efficiency).isCloseTo(expectedEfficiency, within(0.001));

		// API overhead ratio should be reasonable (can be > 1.0 if API processing exceeds
		// total measured time)
		double apiOverhead = metadata.getApiOverheadRatio();
		assertThat(apiOverhead).isGreaterThanOrEqualTo(0.0); // Should be non-negative,
																// but can exceed 1.0

		// Test business logic
		boolean expensive = metadata.isExpensive();
		assertThat(expensive).isEqualTo(metadata.cost().calculateTotal() > 0.10);

		// Test metrics map export
		var metricsMap = metadata.toMetricsMap();
		assertThat(metricsMap).isNotNull();
		assertThat(metricsMap).containsKeys("model", "totalCost", "totalTokens", "durationMs", "efficiency");
		assertThat(metricsMap.get("totalCost")).isEqualTo(metadata.cost().calculateTotal());
		assertThat(metricsMap.get("totalTokens")).isEqualTo(metadata.usage().getTotalTokens());
		assertThat(metricsMap.get("durationMs")).isEqualTo(reportedDuration);

		logger.info("Metadata validation passed - Model: {}, Duration: {}ms, Efficiency: {} tokens/ms",
				metadata.model(), reportedDuration, efficiency);
	}

	@Test
	void testQueryResultConvenienceMethodsWithRealData() throws Exception {
		logger.info("Testing QueryResult convenience methods with real Claude CLI response");

		CLIOptions options = CLIOptions.builder()
			.timeout(Duration.ofMinutes(3)) // AgentClient overhead requires longer
											// timeout
			.build();

		QueryResult result = Query.execute("Hello! Please say hello back and tell me your name.", options);

		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();

		// Test message count
		int messageCount = result.getMessageCount();
		assertThat(messageCount).isEqualTo(result.messages().size());
		assertThat(messageCount).isGreaterThan(0);

		// Test first assistant response
		var firstResponse = result.getFirstAssistantResponse();
		assertThat(firstResponse).isPresent();
		assertThat(firstResponse.get()).isNotEmpty();

		// Test message filtering
		var assistantMessages = result.getAssistantMessages();
		var userMessages = result.getUserMessages();

		assertThat(assistantMessages).isNotEmpty();
		// Note: CLI --print mode doesn't include user messages in response stream
		// User message is implicit in the command itself

		// Validate message types
		for (var msg : assistantMessages) {
			assertThat(msg).isInstanceOf(AssistantMessage.class);
		}

		// Only validate user messages if they exist (not expected in CLI --print mode)
		for (var msg : userMessages) {
			assertThat(msg).isInstanceOf(UserMessage.class);
		}

		// Test result message
		var resultMessage = result.getResultMessage();
		if (resultMessage.isPresent()) {
			assertThat(resultMessage.get()).isInstanceOf(ResultMessage.class);
		}

		// Test tool usage (should be false for simple query)
		boolean hasToolUse = result.hasToolUse();
		var toolUses = result.getAllToolUses();

		if (hasToolUse) {
			assertThat(toolUses).isNotEmpty();
		}
		else {
			assertThat(toolUses).isEmpty();
		}

		// Test performance indicators
		boolean fastResponse = result.isFastResponse();
		boolean expensive = result.isExpensive();

		// These should be consistent with metadata
		assertThat(expensive).isEqualTo(result.metadata().cost().calculateTotal() > 0.10);

		logger.info("QueryResult validation passed - Messages: {}, HasTools: {}, Fast: {}, Expensive: {}", messageCount,
				hasToolUse, fastResponse, expensive);
	}

	@Test
	void testEdgeCasesWithEmptyAndErrorResponses() throws Exception {
		logger.info("Testing edge cases with various response types");

		try {
			// Test with very short timeout to potentially trigger timeout
			CLIOptions shortTimeout = CLIOptions.builder().timeout(Duration.ofMillis(100)).build();

			QueryResult result = Query.execute("Write a very long essay", shortTimeout);

			// If it succeeds despite short timeout, validate the result
			if (result.isSuccessful()) {
				assertThat(result.metadata()).isNotNull();
				assertThat(result.metadata().cost().calculateTotal()).isGreaterThanOrEqualTo(0.0);
				assertThat(result.metadata().usage().getTotalTokens()).isGreaterThanOrEqualTo(0);
			}

		}
		catch (Exception e) {
			// Expected for very short timeout
			logger.info("Expected timeout exception occurred: {}", e.getClass().getSimpleName());
		}

		// Test with minimal prompt
		CLIOptions minimalOptions = CLIOptions.builder()
			.timeout(Duration.ofMinutes(3)) // AgentClient overhead requires longer
											// timeout
			.build();

		QueryResult minimalResult = Query.execute("Hi", minimalOptions);
		assertThat(minimalResult).isNotNull();
		assertThat(minimalResult.metadata().usage().inputTokens()).isGreaterThan(0);
		assertThat(minimalResult.metadata().usage().outputTokens()).isGreaterThan(0);

		logger.info("Edge case validation completed");
	}

	@Test
	void testInteractiveSessionWithMultipleQueries() throws Exception {
		logger.info("Testing interactive session with multiple queries");

		try (ClaudeAgentClient client = ClaudeAgentClient.create(CLIOptions.defaultOptions(), workingDirectory(),
				getClaudeCliPath())) {
			client.connect();
			assertThat(client.isConnected()).isTrue();

			// First query
			QueryResult result1 = client.query("What is 5+5?");
			assertThat(result1.isSuccessful()).isTrue();

			Cost cost1 = result1.metadata().cost();
			Usage usage1 = result1.metadata().usage();

			// Second query in same session
			QueryResult result2 = client.query("What is 10*2?");
			assertThat(result2.isSuccessful()).isTrue();

			Cost cost2 = result2.metadata().cost();
			Usage usage2 = result2.metadata().usage();

			// Validate both results have proper cost and usage data
			assertThat(cost1.calculateTotal()).isGreaterThan(0.0);
			assertThat(cost2.calculateTotal()).isGreaterThan(0.0);
			assertThat(usage1.getTotalTokens()).isGreaterThan(0);
			assertThat(usage2.getTotalTokens()).isGreaterThan(0);

			// Note: Each CLI --print invocation creates a new session, so session IDs
			// will be different
			// In true interactive mode, sessions would be maintained, but not in --print
			// mode
			assertThat(result1.metadata().sessionId()).isNotNull();
			assertThat(result2.metadata().sessionId()).isNotNull();

			logger.info("Interactive session validation passed - Query1 cost: ${}, Query2 cost: ${}",
					cost1.calculateTotal(), cost2.calculateTotal());
		}
	}

	@Test
	void testStreamingCalculationsAccuracy() throws Exception {
		logger.info("Testing streaming response calculations");

		try (ClaudeAgentClient client = ClaudeAgentClient.create(CLIOptions.defaultOptions(), workingDirectory(),
				getClaudeCliPath())) {
			client.connect();

			AtomicInteger messageCount = new AtomicInteger(0);

			// Stream a response and count messages
			client.queryStreaming("Tell me a short story about a robot", message -> {
				messageCount.incrementAndGet();
				assertThat(message).isNotNull();
			});

			assertThat(messageCount.get()).isGreaterThan(0);

			// Now test with reactive streams
			var messageFlux = client.queryStreamAsync("Count from 1 to 5");

			var collectedMessages = messageFlux.collectList().block();
			assertThat(collectedMessages).isNotNull();
			assertThat(collectedMessages.size()).isGreaterThan(0);

			logger.info("Streaming validation passed - Callback messages: {}, Reactive messages: {}",
					messageCount.get(), collectedMessages.size());
		}
	}

}