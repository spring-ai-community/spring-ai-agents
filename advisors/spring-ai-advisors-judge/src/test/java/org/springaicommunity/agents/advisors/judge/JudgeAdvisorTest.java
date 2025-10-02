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

package org.springaicommunity.agents.advisors.judge;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.Goal;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springaicommunity.agents.judge.Judge;
import org.springaicommunity.agents.judge.JudgeMetadata;
import org.springaicommunity.agents.judge.JudgeType;
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentGenerationMetadata;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeAdvisorTest {

	@Test
	void builderRequiresJudge() {
		assertThatThrownBy(() -> JudgeAdvisor.builder().build()).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Judge must be specified");
	}

	@Test
	void constructorRequiresJudge() {
		assertThatThrownBy(() -> new JudgeAdvisor(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Judge cannot be null");
	}

	@Test
	void adviseCallExecutesJudgeAndAttachesJudgment() {
		// Create a simple test judge
		Judge testJudge = new TestJudge(true);

		JudgeAdvisor advisor = JudgeAdvisor.builder().judge(testJudge).build();

		// Create test request and response
		AgentClientRequest request = createTestRequest();
		AgentClientResponse expectedResponse = createSuccessfulResponse();

		// Mock chain to return the response
		AgentCallAdvisorChain chain = mock(AgentCallAdvisorChain.class);
		when(chain.nextCall(any())).thenReturn(expectedResponse);

		// Execute advisor
		AgentClientResponse response = advisor.adviseCall(request, chain);

		// Verify chain was called
		verify(chain).nextCall(request);

		// Verify judgment attached to context (backward compatibility)
		assertThat(response.context()).containsKey("judgment");
		assertThat(response.context()).containsKey("judgment.pass");
		assertThat(response.context()).containsKey("judgment.score");

		// Verify first-class accessor
		Judgment judgment = response.getJudgment();
		assertThat(judgment).isNotNull();
		assertThat(judgment.pass()).isTrue();
		assertThat(response.isJudgmentPassed()).isTrue();
	}

	@Test
	void adviseCallWithFailingJudge() {
		// Create a failing test judge
		Judge testJudge = new TestJudge(false);

		JudgeAdvisor advisor = JudgeAdvisor.builder().judge(testJudge).build();

		AgentClientRequest request = createTestRequest();
		AgentClientResponse expectedResponse = createSuccessfulResponse();

		AgentCallAdvisorChain chain = mock(AgentCallAdvisorChain.class);
		when(chain.nextCall(any())).thenReturn(expectedResponse);

		AgentClientResponse response = advisor.adviseCall(request, chain);

		Judgment judgment = response.getJudgment();
		assertThat(judgment.pass()).isFalse();
		assertThat(response.isJudgmentPassed()).isFalse();
	}

	@Test
	void getNameIncludesJudgeName() {
		Judge testJudge = org.springaicommunity.agents.judge.Judges.named(new TestJudge(true), "TestJudge");
		JudgeAdvisor advisor = new JudgeAdvisor(testJudge);

		assertThat(advisor.getName()).isEqualTo("JudgeAdvisor[TestJudge]");
	}

	@Test
	void defaultOrderIsAfterAgentExecution() {
		Judge testJudge = new TestJudge(true);
		JudgeAdvisor advisor = new JudgeAdvisor(testJudge);

		assertThat(advisor.getOrder()).isEqualTo(JudgeAdvisor.DEFAULT_AGENT_PRECEDENCE_ORDER + 100);
	}

	@Test
	void customOrderCanBeSet() {
		Judge testJudge = new TestJudge(true);
		JudgeAdvisor advisor = JudgeAdvisor.builder().judge(testJudge).order(5000).build();

		assertThat(advisor.getOrder()).isEqualTo(5000);
	}

	@Test
	void judgmentContextBuiltFromRequestAndResponse() {
		// Create a judge that captures the context
		ContextCapturingJudge capturingJudge = new ContextCapturingJudge();

		JudgeAdvisor advisor = new JudgeAdvisor(capturingJudge);

		AgentClientRequest request = createTestRequest();
		AgentClientResponse expectedResponse = createSuccessfulResponse();

		AgentCallAdvisorChain chain = mock(AgentCallAdvisorChain.class);
		when(chain.nextCall(any())).thenReturn(expectedResponse);

		advisor.adviseCall(request, chain);

		// Verify context was built correctly
		JudgmentContext context = capturingJudge.capturedContext;
		assertThat(context).isNotNull();
		assertThat(context.goal()).isEqualTo("Test goal");
		assertThat(context.workspace()).isEqualTo(Path.of("/tmp/test"));
		assertThat(context.agentOutput()).isPresent();
		assertThat(context.executionTime()).isNotNull();
		assertThat(context.startedAt()).isNotNull();
		assertThat(context.status()).isEqualTo(AgentExecutionStatus.SUCCESS);
	}

	// Helper methods and test implementations

	private AgentClientRequest createTestRequest() {
		return new AgentClientRequest(new Goal("Test goal"), Path.of("/tmp/test"), null);
	}

	private AgentClientResponse createSuccessfulResponse() {
		AgentGenerationMetadata metadata = new AgentGenerationMetadata("SUCCESS", null);

		AgentGeneration generation = new AgentGeneration("Agent output", metadata);
		AgentResponse agentResponse = new AgentResponse(List.of(generation));

		return new AgentClientResponse(agentResponse);
	}

	/**
	 * Simple test judge that always returns the same pass/fail result.
	 */
	private static class TestJudge implements Judge {

		private final boolean pass;

		TestJudge(boolean pass) {
			this.pass = pass;
		}

		@Override
		public Judgment judge(JudgmentContext context) {
			return Judgment.builder()
				.score(new BooleanScore(pass))
				.pass(pass)
				.reasoning(pass ? "Test passed" : "Test failed")
				.build();
		}

	}

	/**
	 * Test judge that captures the context for verification.
	 */
	private static class ContextCapturingJudge implements Judge {

		JudgmentContext capturedContext;

		@Override
		public Judgment judge(JudgmentContext context) {
			this.capturedContext = context;
			return Judgment.builder().score(new BooleanScore(true)).pass(true).reasoning("Context captured").build();
		}

	}

}
