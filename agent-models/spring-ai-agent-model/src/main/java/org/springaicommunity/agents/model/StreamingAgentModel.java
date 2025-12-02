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

import reactor.core.publisher.Flux;

/**
 * Functional interface for reactive/streaming agent execution. Returns a Flux that emits
 * responses as the agent progresses through task execution.
 *
 * <p>
 * This is one of three programming models for agent execution:
 * </p>
 * <ul>
 * <li>{@link AgentModel} - Blocking/imperative</li>
 * <li>{@link StreamingAgentModel} - Reactive with Flux (this interface)</li>
 * <li>{@link IterableAgentModel} - Iterator/callback based</li>
 * </ul>
 *
 * <p>
 * As a functional interface, it can be used with lambdas:
 * </p>
 * <pre>{@code
 * StreamingAgentModel agent = request -> myClient.stream(request);
 * agent.stream(request)
 *     .doOnNext(response -> log.info("Step: {}", response.getText()))
 *     .blockLast();
 * }</pre>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Stream task execution with progress updates
 * streamingAgent.stream(request)
 *     .doOnNext(response -> log.info("Step: {}", response.getText()))
 *     .doOnNext(response -> ui.updateProgress(response))
 *     .blockLast();
 *
 * // Cancel long-running task based on intermediate results
 * streamingAgent.stream(request)
 *     .takeUntil(response -> response.getText().contains("error"))
 *     .subscribe();
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
@FunctionalInterface
public interface StreamingAgentModel {

	/**
	 * Execute a development task with streaming intermediate results. This method returns
	 * a Flux that emits responses as the agent progresses through execution.
	 * @param request the task request containing goal, workspace, and constraints
	 * @return a Flux of agent responses representing intermediate execution states
	 */
	Flux<AgentResponse> stream(AgentTaskRequest request);

	/**
	 * Check if the agent is available and ready to accept tasks. Implementations may
	 * override this to perform actual availability checks.
	 * @return true if the agent is available, false otherwise
	 */
	default boolean isAvailable() {
		return true;
	}

}