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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springaicommunity.agents.model.AgentResponse;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for advisor ordering behavior in {@link DefaultAgentCallAdvisorChain}.
 *
 * @author Mark Pollack
 */
class AdvisorOrderingTests {

	@Test
	void advisorsExecuteInOrderDefinedByOrderedInterface() {
		List<String> executionOrder = new ArrayList<>();

		// Create advisors with explicit order values
		AgentCallAdvisor highPriority = createOrderedAdvisor("high", Ordered.HIGHEST_PRECEDENCE, executionOrder);
		AgentCallAdvisor mediumPriority = createOrderedAdvisor("medium", 0, executionOrder);
		AgentCallAdvisor lowPriority = createOrderedAdvisor("low", Ordered.LOWEST_PRECEDENCE - 1, executionOrder);
		AgentCallAdvisor terminal = createOrderedAdvisor("terminal", Ordered.LOWEST_PRECEDENCE, executionOrder);

		// Add in random order to verify sorting
		List<AgentCallAdvisor> advisors = List.of(lowPriority, terminal, highPriority, mediumPriority);
		DefaultAgentCallAdvisorChain chain = DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build();

		chain.nextCall(mock(AgentClientRequest.class));

		// Should execute in priority order: high -> medium -> low -> terminal
		assertThat(executionOrder).containsExactly("high", "medium", "low", "terminal");
	}

	@Test
	void advisorsWithSameOrderExecuteStably() {
		List<String> executionOrder = new ArrayList<>();

		// Create advisors with same order value
		AgentCallAdvisor advisor1 = createOrderedAdvisor("first", 100, executionOrder);
		AgentCallAdvisor advisor2 = createOrderedAdvisor("second", 100, executionOrder);
		AgentCallAdvisor advisor3 = createOrderedAdvisor("third", 100, executionOrder);
		AgentCallAdvisor terminal = createOrderedAdvisor("terminal", Ordered.LOWEST_PRECEDENCE, executionOrder);

		// Add all at once (not push one by one)
		List<AgentCallAdvisor> advisors = List.of(advisor1, advisor2, advisor3, terminal);
		DefaultAgentCallAdvisorChain chain = DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build();

		chain.nextCall(mock(AgentClientRequest.class));

		// Terminal should always be last, others maintain stable order
		assertThat(executionOrder).hasSize(4);
		assertThat(executionOrder.get(3)).isEqualTo("terminal");
	}

	@Test
	void defaultAgentPrecedenceOrderIsCorrect() {
		// Verify the constant value matches Spring AI's pattern
		assertThat(AgentCallAdvisor.DEFAULT_AGENT_PRECEDENCE_ORDER).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1000);
	}

	@Test
	void advisorsWithDefaultPrecedenceExecuteAfterHigherPriority() {
		List<String> executionOrder = new ArrayList<>();

		AgentCallAdvisor highPriority = createOrderedAdvisor("high", Ordered.HIGHEST_PRECEDENCE, executionOrder);
		AgentCallAdvisor defaultPriority = createOrderedAdvisor("default",
				AgentCallAdvisor.DEFAULT_AGENT_PRECEDENCE_ORDER, executionOrder);
		AgentCallAdvisor terminal = createOrderedAdvisor("terminal", Ordered.LOWEST_PRECEDENCE, executionOrder);

		List<AgentCallAdvisor> advisors = List.of(terminal, defaultPriority, highPriority);
		DefaultAgentCallAdvisorChain chain = DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build();

		chain.nextCall(mock(AgentClientRequest.class));

		assertThat(executionOrder).containsExactly("high", "default", "terminal");
	}

	@Test
	void terminalAdvisorWithLowestPrecedenceAlwaysExecutesLast() {
		List<String> executionOrder = new ArrayList<>();

		AgentCallAdvisor advisor1 = createOrderedAdvisor("advisor1", 0, executionOrder);
		AgentCallAdvisor advisor2 = createOrderedAdvisor("advisor2", 500, executionOrder);
		AgentCallAdvisor advisor3 = createOrderedAdvisor("advisor3", 1000, executionOrder);
		AgentCallAdvisor terminal = createOrderedAdvisor("terminal", Integer.MAX_VALUE, executionOrder);

		// Add terminal first to verify it still executes last
		List<AgentCallAdvisor> advisors = List.of(terminal, advisor2, advisor1, advisor3);
		DefaultAgentCallAdvisorChain chain = DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build();

		chain.nextCall(mock(AgentClientRequest.class));

		assertThat(executionOrder).containsExactly("advisor1", "advisor2", "advisor3", "terminal");
		assertThat(executionOrder.get(executionOrder.size() - 1)).isEqualTo("terminal");
	}

	private AgentCallAdvisor createOrderedAdvisor(String name, int order, List<String> executionOrder) {
		return new AgentCallAdvisor() {
			@Override
			public AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain) {
				executionOrder.add(name);
				if (order == Integer.MAX_VALUE || order == Ordered.LOWEST_PRECEDENCE) {
					// Terminal advisor
					return new AgentClientResponse(mock(AgentResponse.class));
				}
				return chain.nextCall(request);
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public int getOrder() {
				return order;
			}
		};
	}

}
