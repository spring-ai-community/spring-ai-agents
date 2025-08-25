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
 * Interface for autonomous development agents that can execute tasks in developer
 * workspaces. This represents the core abstraction for CLI-based agents in Spring AI that
 * handle a broad range of developer tasks beyond just code editing.
 *
 * <p>
 * Unlike chat models that engage in conversations, agent models are task-oriented and
 * work within defined workspaces with file access controls. They can perform various
 * developer tasks including:
 * </p>
 *
 * <ul>
 * <li>Code analysis and modification</li>
 * <li>Git operations (commits, branches, diffs)</li>
 * <li>Test generation and execution</li>
 * <li>Documentation creation and updates</li>
 * <li>Build and dependency management</li>
 * <li>Code review and refactoring</li>
 * <li>Issue classification and triage</li>
 * <li>Configuration file management</li>
 * <li>And other development workflow tasks</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Fix a failing test
 * var result = agent.call(new AgentTaskRequest.builder()
 *     .goal("Fix the failing test in UserServiceTest")
 *     .workingDirectory(projectRoot)
 *     .build());
 *
 * // Create documentation
 * var result = agent.call(new AgentTaskRequest.builder()
 *     .goal("Generate API documentation for the UserController")
 *     .workingDirectory(projectRoot)
 *     .build());
 *
 * // Git operations
 * var result = agent.call(new AgentTaskRequest.builder()
 *     .goal("Create a well-structured commit for the authentication changes")
 *     .workingDirectory(projectRoot)
 *     .build());
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public interface AgentModel {

	/**
	 * Execute a development task using the agent. This is a blocking operation that waits
	 * for the agent to complete the task.
	 * @param request the task request containing goal, workspace, and constraints
	 * @return the result of the agent execution
	 */
	AgentResponse call(AgentTaskRequest request);

	/**
	 * Check if the agent is available and ready to accept tasks.
	 * @return true if the agent is available, false otherwise
	 */
	boolean isAvailable();

}