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

package org.springaicommunity.agents.judge.context;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rich context containing all information needed for judgment.
 *
 * <p>
 * This record encapsulates the agent's goal, workspace, execution result, timing
 * information, and any additional context needed for evaluation. Judges extract the
 * information they need from this context.
 * </p>
 *
 * @param goal the agent's goal or task description
 * @param workspace the workspace directory where the agent executed
 * @param executionTime how long the agent took to execute
 * @param startedAt when the agent started executing
 * @param agentOutput the agent's output (if successful)
 * @param status the agent's execution status
 * @param error any exception that occurred during execution
 * @param metadata additional context information (extensibility)
 * @author Mark Pollack
 * @since 0.1.0
 */
public record JudgmentContext(String goal, Path workspace, Duration executionTime, Instant startedAt,
		Optional<String> agentOutput, AgentExecutionStatus status, Optional<Throwable> error,
		Map<String, Object> metadata) {

	public JudgmentContext {
		metadata = Map.copyOf(metadata);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String goal;

		private Path workspace;

		private Duration executionTime;

		private Instant startedAt;

		private Optional<String> agentOutput = Optional.empty();

		private AgentExecutionStatus status = AgentExecutionStatus.UNKNOWN;

		private Optional<Throwable> error = Optional.empty();

		private Map<String, Object> metadata = new HashMap<>();

		public Builder goal(String goal) {
			this.goal = goal;
			return this;
		}

		public Builder workspace(Path workspace) {
			this.workspace = workspace;
			return this;
		}

		public Builder executionTime(Duration executionTime) {
			this.executionTime = executionTime;
			return this;
		}

		public Builder startedAt(Instant startedAt) {
			this.startedAt = startedAt;
			return this;
		}

		public Builder agentOutput(String agentOutput) {
			this.agentOutput = Optional.ofNullable(agentOutput);
			return this;
		}

		public Builder status(AgentExecutionStatus status) {
			this.status = status;
			return this;
		}

		public Builder error(Throwable error) {
			this.error = Optional.ofNullable(error);
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = new HashMap<>(metadata);
			return this;
		}

		public Builder metadata(String key, Object value) {
			this.metadata.put(key, value);
			return this;
		}

		public JudgmentContext build() {
			return new JudgmentContext(goal, workspace, executionTime, startedAt, agentOutput, status, error, metadata);
		}

	}

}
