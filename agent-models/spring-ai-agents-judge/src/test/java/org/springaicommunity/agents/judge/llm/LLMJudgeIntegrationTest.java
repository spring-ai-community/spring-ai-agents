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

package org.springaicommunity.agents.judge.llm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.result.JudgmentStatus;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springaicommunity.agents.judge.score.NumericalScore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LLMJudge} using OpenAI.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
@SpringBootTest(classes = LLMJudgeIntegrationTest.TestConfig.class)
@EnabledIfEnvironmentVariable(named = "SPRING_AI_OPENAI_API_KEY", matches = ".+")
class LLMJudgeIntegrationTest {

	private static final Pattern PASS_PATTERN = Pattern.compile("PASS:\\s*(true|false)", Pattern.CASE_INSENSITIVE);

	private static final Pattern SCORE_PATTERN = Pattern.compile("SCORE:\\s*([0-9.]+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern REASONING_PATTERN = Pattern.compile("REASONING:\\s*(.+)",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	@Autowired
	private ChatClient.Builder chatClientBuilder;

	@Test
	void shouldEvaluateCodeQualityWithOpenAI() {
		// Create code quality judge
		CodeQualityJudge judge = new CodeQualityJudge(chatClientBuilder);

		// Create judgment context with sample code
		JudgmentContext context = JudgmentContext.builder()
			.goal("Write a function to calculate factorial")
			.workspace(Path.of("/tmp/test"))
			.agentOutput("""
					def factorial(n):
					    if n == 0:
					        return 1
					    return n * factorial(n - 1)
					""")
			.executionTime(Duration.ofSeconds(5))
			.startedAt(Instant.now())
			.status(AgentExecutionStatus.SUCCESS)
			.build();

		// Execute judgment
		Judgment judgment = judge.judge(context);

		// Verify judgment structure
		assertThat(judgment).isNotNull();
		assertThat(judgment.status()).isIn(JudgmentStatus.PASS, JudgmentStatus.FAIL);
		assertThat(judgment.reasoning()).isNotEmpty();
		assertThat(judgment.score()).isNotNull();

		// Log results for manual inspection
		System.out.println("Code Quality Judgment:");
		System.out.println("Status: " + judgment.status());
		System.out.println("Score: " + judgment.score());
		System.out.println("Reasoning: " + judgment.reasoning());
	}

	@Test
	void shouldEvaluateBugDetectionWithOpenAI() {
		// Create bug detection judge
		BugDetectionJudge judge = new BugDetectionJudge(chatClientBuilder);

		// Create judgment context with buggy code
		JudgmentContext context = JudgmentContext.builder()
			.goal("Write a function to divide two numbers")
			.workspace(Path.of("/tmp/test"))
			.agentOutput("""
					def divide(a, b):
					    return a / b
					""")
			.executionTime(Duration.ofSeconds(3))
			.startedAt(Instant.now())
			.status(AgentExecutionStatus.SUCCESS)
			.build();

		// Execute judgment
		Judgment judgment = judge.judge(context);

		// Verify judgment structure
		assertThat(judgment).isNotNull();
		assertThat(judgment.status()).isNotNull();
		assertThat(judgment.reasoning()).isNotEmpty();

		// Should likely detect division by zero issue
		System.out.println("Bug Detection Judgment:");
		System.out.println("Status: " + judgment.status());
		System.out.println("Reasoning: " + judgment.reasoning());
	}

	// ==================== Test Configuration ====================

	@Configuration
	@EnableAutoConfiguration
	static class TestConfig {

		@Bean
		ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
			return ChatClient.builder(chatModel);
		}

	}

	// ==================== Test Judge Implementations ====================

	/**
	 * Code quality judge implementation.
	 */
	static class CodeQualityJudge extends LLMJudge {

		CodeQualityJudge(ChatClient.Builder chatClientBuilder) {
			super("CodeQuality", "Evaluates code quality and best practices", chatClientBuilder);
		}

		@Override
		protected String buildPrompt(JudgmentContext context) {
			return String.format("""
					Evaluate the following code for quality and best practices.

					Goal: %s
					Code:
					%s

					Provide your judgment in this format:
					PASS: true/false (true if code quality is good, false otherwise)
					SCORE: X.X (0-10, where 0=terrible, 10=excellent)
					REASONING: Your detailed explanation

					Consider: correctness, readability, efficiency, error handling, and best practices.
					""", context.goal(), context.agentOutput().orElse("No code provided"));
		}

		@Override
		protected Judgment parseResponse(String response, JudgmentContext context) {
			boolean pass = extractPass(response);
			Double score = extractScore(response);
			String reasoning = extractReasoning(response);

			Judgment.Builder builder = Judgment.builder()
				.status(pass ? JudgmentStatus.PASS : JudgmentStatus.FAIL)
				.reasoning(reasoning);

			if (score != null) {
				builder.score(new NumericalScore(score, 0.0, 10.0));
			}
			else {
				builder.score(new BooleanScore(pass));
			}

			return builder.build();
		}

	}

	/**
	 * Bug detection judge implementation.
	 */
	static class BugDetectionJudge extends LLMJudge {

		BugDetectionJudge(ChatClient.Builder chatClientBuilder) {
			super("BugDetection", "Identifies potential bugs and issues", chatClientBuilder);
		}

		@Override
		protected String buildPrompt(JudgmentContext context) {
			return String.format("""
					Analyze the following code for potential bugs and issues.

					Goal: %s
					Code:
					%s

					Provide your judgment in this format:
					PASS: true/false (true if no critical bugs found, false if bugs detected)
					SCORE: X.X (0-10, where 0=many critical bugs, 10=no bugs)
					REASONING: List any bugs or issues you find, or confirm code is bug-free

					Look for: logic errors, edge cases, null handling, resource leaks, etc.
					""", context.goal(), context.agentOutput().orElse("No code provided"));
		}

		@Override
		protected Judgment parseResponse(String response, JudgmentContext context) {
			boolean pass = extractPass(response);
			Double score = extractScore(response);
			String reasoning = extractReasoning(response);

			Judgment.Builder builder = Judgment.builder()
				.status(pass ? JudgmentStatus.PASS : JudgmentStatus.FAIL)
				.reasoning(reasoning);

			if (score != null) {
				builder.score(new NumericalScore(score, 0.0, 10.0));
			}
			else {
				builder.score(new BooleanScore(pass));
			}

			return builder.build();
		}

	}

	// ==================== Helper Methods ====================

	private static boolean extractPass(String output) {
		Matcher matcher = PASS_PATTERN.matcher(output);
		if (matcher.find()) {
			return Boolean.parseBoolean(matcher.group(1));
		}
		return false;
	}

	private static Double extractScore(String output) {
		Matcher matcher = SCORE_PATTERN.matcher(output);
		if (matcher.find()) {
			try {
				return Double.parseDouble(matcher.group(1));
			}
			catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static String extractReasoning(String output) {
		Matcher matcher = REASONING_PATTERN.matcher(output);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return output;
	}

}
