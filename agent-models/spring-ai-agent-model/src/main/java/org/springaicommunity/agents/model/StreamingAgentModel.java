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
 * Extension of {@link AgentModel} that supports streaming of intermediate task execution
 * steps. This allows real-time monitoring of agent progress during long-running tasks.
 *
 * <p>
 * Streaming agent models emit intermediate states, tool calls, reasoning steps, or
 * partial results as the agent progresses through task execution. This is particularly
 * useful for:
 * </p>
 *
 * <ul>
 * <li>Long-running development tasks (e.g., large refactoring, test generation)</li>
 * <li>Interactive development workflows with human oversight</li>
 * <li>Progress monitoring and early termination capabilities</li>
 * <li>Real-time feedback in development tools and IDEs</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Stream task execution with progress updates
 * streamingAgent.stream(request)
 *     .doOnNext(delta -> log.info("Step: {}", delta.getStep()))
 *     .doOnNext(delta -> ui.updateProgress(delta.getProgress()))
 *     .blockLast(); // Wait for completion
 *
 * // Cancel long-running task based on intermediate results
 * streamingAgent.stream(request)
 *     .takeUntil(delta -> delta.getStep().contains("critical_error"))
 *     .subscribe();
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public interface StreamingAgentModel extends AgentModel {

	/**
	 * Execute a development task with streaming intermediate results. This method returns
	 * a Flux that emits responses as the agent progresses through execution.
	 * @param request the task request containing goal, workspace, and constraints
	 * @return a Flux of agent responses representing intermediate execution states
	 */
	Flux<AgentResponse> stream(AgentTaskRequest request);

}