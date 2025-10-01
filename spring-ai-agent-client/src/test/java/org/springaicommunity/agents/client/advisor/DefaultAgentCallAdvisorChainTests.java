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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.Goal;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultAgentCallAdvisorChain}.
 *
 * @author Mark Pollack
 */
class DefaultAgentCallAdvisorChainTests {

	@Test
	void whenAdvisorIsNullThenThrow() {
		assertThatThrownBy(() -> DefaultAgentCallAdvisorChain.builder().push(null).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("the advisor must be non-null");
	}

	@Test
	void whenAdvisorListIsNullThenThrow() {
		assertThatThrownBy(() -> DefaultAgentCallAdvisorChain.builder().pushAll(null).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("the advisors must be non-null");
	}

	@Test
	void whenAdvisorListContainsNullElementsThenThrow() {
		List<AgentCallAdvisor> advisors = new ArrayList<>();
		advisors.add(null);
		assertThatThrownBy(() -> DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("the advisors must not contain null elements");
	}

	@Test
	void whenNoAdvisorsAvailableThenThrow() {
		AgentCallAdvisorChain chain = DefaultAgentCallAdvisorChain.builder().build();
		AgentClientRequest request = new AgentClientRequest(new Goal("test"), Path.of("."), mock(AgentOptions.class));

		assertThatThrownBy(() -> chain.nextCall(request)).isInstanceOf(IllegalStateException.class)
			.hasMessage("No AgentCallAdvisors available to execute");
	}

	@Test
	void whenRequestIsNullThenThrow() {
		AgentCallAdvisor mockAdvisor = mock(AgentCallAdvisor.class);
		when(mockAdvisor.getName()).thenReturn("advisor1");

		AgentCallAdvisorChain chain = DefaultAgentCallAdvisorChain.builder().push(mockAdvisor).build();

		assertThatThrownBy(() -> chain.nextCall(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("the request cannot be null");
	}

	@Test
	void getCallAdvisors() {
		AgentCallAdvisor mockAdvisor1 = mock(AgentCallAdvisor.class);
		when(mockAdvisor1.getName()).thenReturn("advisor1");
		when(mockAdvisor1.adviseCall(any(), any())).thenReturn(new AgentClientResponse(mock(AgentResponse.class)));

		AgentCallAdvisor mockAdvisor2 = mock(AgentCallAdvisor.class);
		when(mockAdvisor2.getName()).thenReturn("advisor2");
		when(mockAdvisor2.adviseCall(any(), any())).thenReturn(new AgentClientResponse(mock(AgentResponse.class)));

		List<AgentCallAdvisor> advisors = List.of(mockAdvisor1, mockAdvisor2);
		AgentCallAdvisorChain chain = DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build();

		assertThat(chain.getCallAdvisors()).containsExactlyInAnyOrder(advisors.toArray(new AgentCallAdvisor[0]));

		AgentClientRequest request = new AgentClientRequest(new Goal("test"), Path.of("."), mock(AgentOptions.class));
		chain.nextCall(request);
		assertThat(chain.getCallAdvisors()).containsExactlyInAnyOrder(advisors.toArray(new AgentCallAdvisor[0]));

		chain.nextCall(request);
		assertThat(chain.getCallAdvisors()).containsExactlyInAnyOrder(advisors.toArray(new AgentCallAdvisor[0]));
	}

	@Test
	void advisorChainExecutesInOrder() {
		List<String> executionOrder = new ArrayList<>();

		AgentCallAdvisor advisor1 = new TestAdvisor("advisor1", 1, executionOrder);
		AgentCallAdvisor advisor2 = new TestAdvisor("advisor2", 2, executionOrder);
		AgentCallAdvisor terminalAdvisor = new TestAdvisor("terminal", Integer.MAX_VALUE, executionOrder);

		List<AgentCallAdvisor> advisors = List.of(advisor1, advisor2, terminalAdvisor);
		AgentCallAdvisorChain chain = DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build();

		AgentClientRequest request = new AgentClientRequest(new Goal("test"), Path.of("."), mock(AgentOptions.class));
		chain.nextCall(request);

		assertThat(executionOrder).containsExactly("advisor1", "advisor2", "terminal");
	}

	/**
	 * Test advisor that records execution order.
	 */
	private static class TestAdvisor implements AgentCallAdvisor {

		private final String name;

		private final int order;

		private final List<String> executionOrder;

		TestAdvisor(String name, int order, List<String> executionOrder) {
			this.name = name;
			this.order = order;
			this.executionOrder = executionOrder;
		}

		@Override
		public AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain) {
			this.executionOrder.add(this.name);
			if (this.order == Integer.MAX_VALUE) {
				// Terminal advisor - return response
				return new AgentClientResponse(mock(AgentResponse.class));
			}
			// Continue chain
			return chain.nextCall(request);
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

}
