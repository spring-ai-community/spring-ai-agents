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

package org.springaicommunity.agents.client.advisor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.Goal;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AgentModelCallAdvisor}.
 *
 * @author Mark Pollack
 */
class AgentModelCallAdvisorTests {

	@Test
	void shouldCallAgentModelWithCorrectRequest() {
		AgentModel agentModel = mock(AgentModel.class);
		ArgumentCaptor<AgentTaskRequest> taskRequestCaptor = ArgumentCaptor.forClass(AgentTaskRequest.class);
		AgentResponse mockResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(taskRequestCaptor.capture())).willReturn(mockResponse);

		AgentModelCallAdvisor advisor = new AgentModelCallAdvisor(agentModel);

		Goal goal = new Goal("Test goal");
		Path workingDir = Path.of("/test/dir");
		AgentOptions options = mock(AgentOptions.class);
		AgentClientRequest request = new AgentClientRequest(goal, workingDir, options, new HashMap<>());

		AgentCallAdvisorChain mockChain = mock(AgentCallAdvisorChain.class);
		AgentClientResponse response = advisor.adviseCall(request, mockChain);

		// Verify the agent model was called
		verify(agentModel).call(any(AgentTaskRequest.class));

		// Verify the task request was constructed correctly
		AgentTaskRequest taskRequest = taskRequestCaptor.getValue();
		assertThat(taskRequest.goal()).isEqualTo("Test goal");
		assertThat(taskRequest.workingDirectory()).isEqualTo(workingDir);
		assertThat(taskRequest.options()).isEqualTo(options);

		// Verify the response wraps the agent response
		assertThat(response.agentResponse()).isEqualTo(mockResponse);
	}

	@Test
	void shouldPreserveContextFromRequest() {
		AgentModel agentModel = mock(AgentModel.class);
		AgentResponse mockResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(mockResponse);

		AgentModelCallAdvisor advisor = new AgentModelCallAdvisor(agentModel);

		Goal goal = new Goal("Test goal");
		Path workingDir = Path.of("/test/dir");
		AgentOptions options = mock(AgentOptions.class);
		HashMap<String, Object> context = new HashMap<>();
		context.put("test-key", "test-value");
		AgentClientRequest request = new AgentClientRequest(goal, workingDir, options, context);

		AgentCallAdvisorChain mockChain = mock(AgentCallAdvisorChain.class);
		AgentClientResponse response = advisor.adviseCall(request, mockChain);

		// Verify context is preserved
		assertThat(response.context()).containsEntry("test-key", "test-value");
	}

	@Test
	void shouldHaveLowestPrecedence() {
		AgentModel agentModel = mock(AgentModel.class);
		AgentModelCallAdvisor advisor = new AgentModelCallAdvisor(agentModel);
		assertThat(advisor.getOrder()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void shouldHaveName() {
		AgentModel agentModel = mock(AgentModel.class);
		AgentModelCallAdvisor advisor = new AgentModelCallAdvisor(agentModel);
		assertThat(advisor.getName()).isEqualTo(AgentModelCallAdvisor.class.getName());
	}

}
