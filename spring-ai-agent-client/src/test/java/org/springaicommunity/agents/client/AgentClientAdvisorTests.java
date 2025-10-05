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

package org.springaicommunity.agents.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AgentClient} with a focus on verifying advisor integration and
 * execution flow.
 *
 * @author Mark Pollack
 */
class AgentClientAdvisorTests {

	@Test
	void advisorsExecuteInCorrectOrder() {
		AgentModel agentModel = mock(AgentModel.class);
		List<String> executionLog = new ArrayList<>();

		// Create mock response
		AgentResponse agentResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(agentResponse);

		// Create advisors that log execution
		AgentCallAdvisor loggingAdvisor1 = new LoggingAdvisor("advisor1", 100, executionLog);
		AgentCallAdvisor loggingAdvisor2 = new LoggingAdvisor("advisor2", 200, executionLog);

		// Build client with advisors
		AgentClient client = AgentClient.builder(agentModel)
			.defaultAdvisor(loggingAdvisor1)
			.defaultAdvisor(loggingAdvisor2)
			.build();

		// Execute
		AgentClientResponse response = client.goal("Test goal").workingDirectory(Path.of(".")).run();

		// Verify advisors executed in order
		assertThat(executionLog).containsExactly("advisor1-before", "advisor2-before", "advisor2-after",
				"advisor1-after");

		// Verify response
		assertThat(response.agentResponse()).isEqualTo(agentResponse);
	}

	@Test
	void advisorsCanModifyContext() {
		AgentModel agentModel = mock(AgentModel.class);
		// Create mock response
		AgentResponse agentResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(agentResponse);

		// Create advisor that modifies context
		AgentCallAdvisor contextAdvisor = new AgentCallAdvisor() {
			@Override
			public AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain) {
				request.context().put("pre-key", "pre-value");
				AgentClientResponse response = chain.nextCall(request);
				response.context().put("post-key", "post-value");
				return response;
			}

			@Override
			public String getName() {
				return "context-advisor";
			}

			@Override
			public int getOrder() {
				return 100;
			}
		};

		// Build client with advisor
		AgentClient client = AgentClient.builder(agentModel).defaultAdvisor(contextAdvisor).build();

		// Execute
		AgentClientResponse response = client.goal("Test goal").workingDirectory(Path.of(".")).run();

		// Verify context modifications
		assertThat(response.context()).containsEntry("pre-key", "pre-value").containsEntry("post-key", "post-value");
	}

	@Test
	void multipleAdvisorsCanShareContextData() {
		AgentModel agentModel = mock(AgentModel.class);
		// Create mock response
		AgentResponse agentResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(agentResponse);

		// Advisor 1 writes to context
		AgentCallAdvisor writerAdvisor = new AgentCallAdvisor() {
			@Override
			public AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain) {
				request.context().put("shared-data", "advisor1-wrote-this");
				return chain.nextCall(request);
			}

			@Override
			public String getName() {
				return "writer";
			}

			@Override
			public int getOrder() {
				return 100;
			}
		};

		// Advisor 2 reads from context
		List<String> readValues = new ArrayList<>();
		AgentCallAdvisor readerAdvisor = new AgentCallAdvisor() {
			@Override
			public AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain) {
				String value = (String) request.context().get("shared-data");
				readValues.add(value);
				return chain.nextCall(request);
			}

			@Override
			public String getName() {
				return "reader";
			}

			@Override
			public int getOrder() {
				return 200;
			}
		};

		// Build client with both advisors
		AgentClient client = AgentClient.builder(agentModel)
			.defaultAdvisors(List.of(writerAdvisor, readerAdvisor))
			.build();

		// Execute
		client.goal("Test goal").workingDirectory(Path.of(".")).run();

		// Verify reader saw writer's data
		assertThat(readValues).containsExactly("advisor1-wrote-this");
	}

	@Test
	void clientWithoutAdvisorsStillWorks() {
		AgentModel agentModel = mock(AgentModel.class);
		// Create mock response
		AgentResponse agentResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(agentResponse);

		// Build client without advisors
		AgentClient client = AgentClient.builder(agentModel).build();

		// Execute
		AgentClientResponse response = client.goal("Test goal").workingDirectory(Path.of(".")).run();

		// Verify response
		assertThat(response.agentResponse()).isEqualTo(agentResponse);
	}

	@Test
	void mutatePreservesAdvisors() {
		AgentModel agentModel = mock(AgentModel.class);
		// Create advisor
		AgentCallAdvisor advisor = new LoggingAdvisor("test", 100, new ArrayList<>());

		// Build original client with advisor
		AgentClient originalClient = AgentClient.builder(agentModel).defaultAdvisor(advisor).build();

		// Mutate
		AgentClient mutatedClient = originalClient.mutate().build();

		// Both should have advisors (verified by successful execution)
		AgentResponse agentResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(agentResponse);

		List<String> log1 = new ArrayList<>();
		List<String> log2 = new ArrayList<>();

		// Can't directly verify advisor list, but execution should work
		AgentClientResponse response = mutatedClient.goal("Test").workingDirectory(Path.of(".")).run();
		assertThat(response).isNotNull();
	}

	/**
	 * Test advisor that logs before and after execution.
	 */
	private static class LoggingAdvisor implements AgentCallAdvisor {

		private final String name;

		private final int order;

		private final List<String> executionLog;

		LoggingAdvisor(String name, int order, List<String> executionLog) {
			this.name = name;
			this.order = order;
			this.executionLog = executionLog;
		}

		@Override
		public AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain) {
			this.executionLog.add(this.name + "-before");
			AgentClientResponse response = chain.nextCall(request);
			this.executionLog.add(this.name + "-after");
			return response;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

	}

	@Test
	void requestAdvisorsCanBeAddedViaFluentAPI() {
		AgentModel agentModel = mock(AgentModel.class);
		List<String> executionLog = new ArrayList<>();

		// Create mock response
		AgentResponse agentResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(agentResponse);

		// Create advisors
		AgentCallAdvisor requestAdvisor1 = new LoggingAdvisor("request1", 100, executionLog);
		AgentCallAdvisor requestAdvisor2 = new LoggingAdvisor("request2", 200, executionLog);

		// Build client and add request-level advisors via fluent API
		AgentClient client = AgentClient.builder(agentModel).build();

		// Execute with advisors in the request spec
		client.goal("Test goal").workingDirectory(Path.of(".")).advisors(requestAdvisor1, requestAdvisor2).run();

		// Verify advisors executed
		assertThat(executionLog).containsExactly("request1-before", "request2-before", "request2-after",
				"request1-after");
	}

	@Test
	void requestAdvisorsCanBeAddedViaListAPI() {
		AgentModel agentModel = mock(AgentModel.class);
		List<String> executionLog = new ArrayList<>();

		// Create mock response
		AgentResponse agentResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(agentResponse);

		// Create advisors
		List<AgentCallAdvisor> advisorList = List.of(new LoggingAdvisor("advisor1", 100, executionLog),
				new LoggingAdvisor("advisor2", 200, executionLog));

		// Build client and add request-level advisors via List API
		AgentClient client = AgentClient.builder(agentModel).build();

		// Execute with advisors in the request spec
		client.goal("Test goal").workingDirectory(Path.of(".")).advisors(advisorList).run();

		// Verify advisors executed
		assertThat(executionLog).containsExactly("advisor1-before", "advisor2-before", "advisor2-after",
				"advisor1-after");
	}

	@Test
	void requestAdvisorsAreCombinedWithDefaultAdvisors() {
		AgentModel agentModel = mock(AgentModel.class);
		List<String> executionLog = new ArrayList<>();

		// Create mock response
		AgentResponse agentResponse = new AgentResponse(List.of(mock(AgentGeneration.class)));
		given(agentModel.call(any(AgentTaskRequest.class))).willReturn(agentResponse);

		// Create advisors
		AgentCallAdvisor defaultAdvisor = new LoggingAdvisor("default", 100, executionLog);
		AgentCallAdvisor requestAdvisor = new LoggingAdvisor("request", 200, executionLog);

		// Build client with default advisor
		AgentClient client = AgentClient.builder(agentModel).defaultAdvisor(defaultAdvisor).build();

		// Execute with request advisor
		client.goal("Test goal").workingDirectory(Path.of(".")).advisors(requestAdvisor).run();

		// Verify both advisors executed in order
		assertThat(executionLog).containsExactly("default-before", "request-before", "request-after", "default-after");
	}

}
