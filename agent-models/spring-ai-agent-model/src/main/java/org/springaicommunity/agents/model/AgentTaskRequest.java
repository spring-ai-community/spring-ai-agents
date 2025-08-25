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

import java.nio.file.Path;

/**
 * Represents a task request for an autonomous agent. This is not chat-based but rather a
 * single-goal execution against a workspace.
 *
 * @param goal the clear task description for the agent
 * @param workingDirectory the workspace root directory
 * @param options agent execution configuration
 * @author Mark Pollack
 * @since 0.1.0
 */
public record AgentTaskRequest(String goal, Path workingDirectory, AgentOptions options) {

	/**
	 * Create a builder for AgentTaskRequest.
	 * @param goal the task goal
	 * @param workingDirectory the working directory
	 * @return a new builder instance
	 */
	public static Builder builder(String goal, Path workingDirectory) {
		return new Builder(goal, workingDirectory);
	}

	/**
	 * Builder for AgentTaskRequest.
	 */
	public static final class Builder {

		private final String goal;

		private final Path workingDirectory;

		private AgentOptions options;

		private Builder(String goal, Path workingDirectory) {
			this.goal = goal;
			this.workingDirectory = workingDirectory;
		}

		/**
		 * Set agent execution options.
		 * @param options the agent options
		 * @return this builder
		 */
		public Builder options(AgentOptions options) {
			this.options = options;
			return this;
		}

		/**
		 * Build the AgentTaskRequest.
		 * @return the constructed request
		 */
		public AgentTaskRequest build() {
			return new AgentTaskRequest(goal, workingDirectory, options);
		}

	}

}