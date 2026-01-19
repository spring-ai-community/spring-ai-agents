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
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.judge.JudgeMetadata;
import org.springaicommunity.judge.JudgeType;

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

}
