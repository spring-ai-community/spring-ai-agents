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
import org.springaicommunity.agents.judge.JudgeMetadata;
import org.springaicommunity.agents.judge.JudgeType;
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.result.JudgmentStatus;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springframework.ai.chat.client.ChatClient;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LLMJudge}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class LLMJudgeTest {

	@Test
	void shouldCreateJudgeWithMetadata() {
		TestLLMJudge judge = new TestLLMJudge("TestJudge", "Test Description", null);

		JudgeMetadata metadata = judge.metadata();

		assertThat(metadata.name()).isEqualTo("TestJudge");
		assertThat(metadata.description()).isEqualTo("Test Description");
		assertThat(metadata.type()).isEqualTo(JudgeType.LLM_POWERED);
	}

	@Test
	void shouldBuildPromptFromContext() {
		TestLLMJudge judge = new TestLLMJudge("TestJudge", "Test", null);

		JudgmentContext context = createTestContext();

		String prompt = judge.buildPrompt(context);

		assertThat(prompt).contains("Test goal");
		assertThat(prompt).contains("Test output");
	}

	@Test
	void shouldParseResponseIntoJudgment() {
		TestLLMJudge judge = new TestLLMJudge("TestJudge", "Test", null);

		JudgmentContext context = createTestContext();
		String response = "Test LLM response";

		Judgment judgment = judge.parseResponse(response, context);

		assertThat(judgment).isNotNull();
		assertThat(judgment.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(judgment.reasoning()).contains("Parsed from LLM");
	}

	@Test
	void shouldAllowNullChatClientForTesting() {
		TestLLMJudge judge = new TestLLMJudge("TestJudge", "Test", null);

		assertThat(judge.chatClient).isNull();
	}

	// ==================== Helper Methods ====================

	private JudgmentContext createTestContext() {
		return JudgmentContext.builder()
			.goal("Test goal")
			.workspace(Path.of("/tmp/test"))
			.agentOutput("Test output")
			.executionTime(Duration.ofSeconds(1))
			.startedAt(Instant.now())
			.status(AgentExecutionStatus.SUCCESS)
			.build();
	}

	/**
	 * Test implementation of LLMJudge for testing the abstract base class.
	 */
	static class TestLLMJudge extends LLMJudge {

		TestLLMJudge(String name, String description, ChatClient.Builder chatClientBuilder) {
			super(name, description, chatClientBuilder);
		}

		@Override
		protected String buildPrompt(JudgmentContext context) {
			return String.format("Evaluate: Goal=%s, Output=%s", context.goal(), context.agentOutput().orElse("None"));
		}

		@Override
		protected Judgment parseResponse(String response, JudgmentContext context) {
			return Judgment.builder()
				.status(JudgmentStatus.PASS)
				.score(new BooleanScore(true))
				.reasoning("Parsed from LLM: " + response)
				.build();
		}

	}

}
