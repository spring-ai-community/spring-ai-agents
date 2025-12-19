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

/**
 * Functional interface for blocking/imperative agent execution. Executes tasks in
 * developer workspaces and returns results synchronously.
 *
 * <p>
 * This is one of three programming models for agent execution:
 * </p>
 * <ul>
 * <li>{@link AgentModel} - Blocking/imperative (this interface)</li>
 * <li>{@link StreamingAgentModel} - Reactive with Flux</li>
 * <li>{@link IterableAgentModel} - Iterator/callback based</li>
 * </ul>
 *
 * <p>
 * As a functional interface, it can be used with lambdas:
 * </p>
 * <pre>{@code
 * AgentModel agent = request -> myClient.execute(request);
 * AgentResponse response = agent.call(request);
 * }</pre>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Fix a failing test
 * var result = agent.call(AgentTaskRequest.builder()
 *     .goal("Fix the failing test in UserServiceTest")
 *     .workingDirectory(projectRoot)
 *     .build());
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
@FunctionalInterface
public interface AgentModel {

	/**
	 * Execute a development task using the agent. This is a blocking operation that waits
	 * for the agent to complete the task.
	 * @param request the task request containing goal, workspace, and constraints
	 * @return the result of the agent execution
	 */
	AgentResponse call(AgentTaskRequest request);

	/**
	 * Check if the agent is available and ready to accept tasks. Implementations may
	 * override this to perform actual availability checks.
	 * @return true if the agent is available, false otherwise
	 */
	default boolean isAvailable() {
		return true;
	}

}