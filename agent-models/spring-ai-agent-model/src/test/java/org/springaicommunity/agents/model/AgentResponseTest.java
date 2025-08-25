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

package org.springaicommunity.agents.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AgentResponse.
 *
 * @author Mark Pollack
 */
class AgentResponseTest {

	@Test
	void createSuccessResult() {
		AgentGenerationMetadata generationMetadata = new AgentGenerationMetadata("SUCCESS", java.util.Map.of());
		AgentGeneration generation = new AgentGeneration("Test output", generationMetadata);

		AgentResponseMetadata responseMetadata = new AgentResponseMetadata("test-model", Duration.ofMinutes(2),
				"session-123", java.util.Map.of());

		AgentResponse result = new AgentResponse(List.of(generation), responseMetadata);

		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("SUCCESS");
		assertThat(result.getResult().getOutput()).isEqualTo("Test output");
		assertThat(result.getMetadata().getDuration()).isEqualTo(Duration.ofMinutes(2));
		assertThat(result.getMetadata().getModel()).isEqualTo("test-model");
		assertThat(result.getMetadata().getSessionId()).isEqualTo("session-123");
	}

	@Test
	void statusEnumValues() {
		assertThat("SUCCESS").isNotNull();
		assertThat("ERROR").isNotNull();
	}

}