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

package org.springaicommunity.agents.judge.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentOptions;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.judge.JudgeMetadata;
import org.springaicommunity.agents.judge.JudgeType;
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.result.JudgmentStatus;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AgentJudge}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class AgentJudgeTest {

	@Test
	void shouldRequireAgentClient() {
		assertThatThrownBy(() -> AgentJudge.builder().criteria("Test criteria").build())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("AgentClient is required");
	}

	@Test
	void shouldRequireCriteria() {
		AgentClient mockClient = mock(AgentClient.class);

		assertThatThrownBy(() -> AgentJudge.builder().agentClient(mockClient).build())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Criteria is required");
	}

	@Test
	void shouldCreateJudgeWithMetadata() {
		AgentClient mockClient = mock(AgentClient.class);

		AgentJudge judge = AgentJudge.builder()
			.agentClient(mockClient)
			.name("TestAgent")
			.description("Test Description")
			.criteria("Test criteria")
			.build();

		JudgeMetadata metadata = judge.metadata();

		assertThat(metadata.name()).isEqualTo("TestAgent");
		assertThat(metadata.description()).isEqualTo("Test Description");
		assertThat(metadata.type()).isEqualTo(JudgeType.AGENT);
	}

	@Test
	void shouldCreateCodeReviewJudge() {
		AgentClient mockClient = mock(AgentClient.class);

		AgentJudge judge = AgentJudge.codeReview(mockClient);

		JudgeMetadata metadata = judge.metadata();
		assertThat(metadata.name()).isEqualTo("CodeReview");
		assertThat(metadata.description()).isEqualTo("Agent-powered code review");
	}

	@Test
	void shouldCreateSecurityAuditJudge() {
		AgentClient mockClient = mock(AgentClient.class);

		AgentJudge judge = AgentJudge.securityAudit(mockClient);

		JudgeMetadata metadata = judge.metadata();
		assertThat(metadata.name()).isEqualTo("SecurityAudit");
		assertThat(metadata.description()).isEqualTo("Agent-powered security audit");
	}

	// ==================== Integration Tests ====================

	/**
	 * Integration test with Gemini agent.
	 */
	@SpringBootTest(classes = AgentJudgeGeminiIntegrationTest.TestConfig.class)
	@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
	static class AgentJudgeGeminiIntegrationTest {

		@Autowired
		private AgentClient agentClient;

		@Test
		void shouldEvaluateCodeWithGeminiAgent() {
			AgentJudge judge = AgentJudge.builder()
				.agentClient(agentClient)
				.name("CodeReview")
				.description("Reviews code quality")
				.criteria("Evaluate if the code correctly implements a factorial function with proper error handling")
				.build();

			JudgmentContext context = JudgmentContext.builder()
				.goal("Implement factorial function")
				.workspace(Path.of("/tmp/test"))
				.agentOutput("""
						def factorial(n):
						    if n < 0:
						        raise ValueError("Factorial not defined for negative numbers")
						    if n == 0:
						        return 1
						    return n * factorial(n - 1)
						""")
				.executionTime(Duration.ofSeconds(5))
				.startedAt(Instant.now())
				.status(AgentExecutionStatus.SUCCESS)
				.build();

			Judgment judgment = judge.judge(context);

			assertThat(judgment).isNotNull();
			assertThat(judgment.status()).isIn(JudgmentStatus.PASS, JudgmentStatus.FAIL);
			assertThat(judgment.reasoning()).isNotEmpty();

			System.out.println("Agent Judge Evaluation:");
			System.out.println("Status: " + judgment.status());
			System.out.println("Score: " + judgment.score());
			System.out.println("Reasoning: " + judgment.reasoning());
		}

		@Configuration
		@EnableAutoConfiguration
		static class TestConfig {

			@Bean
			public Sandbox sandbox() {
				return new org.springaicommunity.agents.model.sandbox.LocalSandbox(Path.of("/tmp"));
			}

			@Bean
			public GeminiClient geminiClient() {
				CLIOptions cliOptions = CLIOptions.builder().model("gemini-2.0-flash-exp").yoloMode(true).build();
				return GeminiClient.create(cliOptions);
			}

			@Bean
			public GeminiAgentOptions geminiAgentOptions() {
				return GeminiAgentOptions.builder().model("gemini-2.0-flash-exp").build();
			}

			@Bean
			public GeminiAgentModel geminiAgentModel(GeminiClient geminiClient, GeminiAgentOptions geminiAgentOptions,
					Sandbox sandbox) {
				return new GeminiAgentModel(geminiClient, geminiAgentOptions, sandbox);
			}

			@Bean
			public AgentClient agentClient(GeminiAgentModel geminiAgentModel) {
				return AgentClient.builder(geminiAgentModel).build();
			}

		}

	}

}
