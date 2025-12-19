/*
 * Copyright 2025 Spring AI Community
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

import java.util.Iterator;

/**
 * Functional interface for iterator-based agent execution. Returns an Iterator that
 * yields responses as the agent progresses through task execution.
 *
 * <p>
 * This is one of three programming models for agent execution:
 * </p>
 * <ul>
 * <li>{@link AgentModel} - Blocking/imperative</li>
 * <li>{@link StreamingAgentModel} - Reactive with Flux</li>
 * <li>{@link IterableAgentModel} - Iterator/callback based (this interface)</li>
 * </ul>
 *
 * <p>
 * As a functional interface, it can be used with lambdas:
 * </p>
 * <pre>{@code
 * IterableAgentModel agent = request -> myClient.iterate(request);
 * for (AgentResponse response : (Iterable<AgentResponse>) () -> agent.iterate(request)) {
 *     log.info("Step: {}", response.getText());
 * }
 * }</pre>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Iterate through responses with for-each
 * Iterator<AgentResponse> iterator = agent.iterate(request);
 * while (iterator.hasNext()) {
 *     AgentResponse response = iterator.next();
 *     log.info("Step: {}", response.getText());
 *     if (response.getText().contains("error")) {
 *         break; // Early termination
 *     }
 * }
 * }</pre>
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
@FunctionalInterface
public interface IterableAgentModel {

	/**
	 * Execute a development task with iterator-based results. This method returns an
	 * Iterator that yields responses as the agent progresses through execution.
	 * @param request the task request containing goal, workspace, and constraints
	 * @return an Iterator of agent responses representing intermediate execution states
	 */
	Iterator<AgentResponse> iterate(AgentTaskRequest request);

	/**
	 * Check if the agent is available and ready to accept tasks. Implementations may
	 * override this to perform actual availability checks.
	 * @return true if the agent is available, false otherwise
	 */
	default boolean isAvailable() {
		return true;
	}

}
