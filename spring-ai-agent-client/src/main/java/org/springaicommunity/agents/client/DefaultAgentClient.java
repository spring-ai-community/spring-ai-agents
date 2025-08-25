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
import java.util.List;
import java.util.Objects;

import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

/**
 * Default implementation of AgentClient following Spring AI patterns.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class DefaultAgentClient implements AgentClient {

	private final AgentModel agentModel;

	private final AgentOptions defaultOptions;

	/**
	 * Create a new DefaultAgentClient with the given agent model.
	 * @param agentModel the underlying agent model
	 */
	public DefaultAgentClient(AgentModel agentModel) {
		this(agentModel, new DefaultAgentOptions());
	}

	/**
	 * Create a new DefaultAgentClient with the given agent model and default options.
	 * @param agentModel the underlying agent model
	 * @param defaultOptions default options for all requests
	 */
	public DefaultAgentClient(AgentModel agentModel, AgentOptions defaultOptions) {
		this.agentModel = Objects.requireNonNull(agentModel, "AgentModel cannot be null");
		this.defaultOptions = defaultOptions != null ? defaultOptions : new DefaultAgentOptions();
	}

	@Override
	public AgentClientRequestSpec goal() {
		return new DefaultAgentClientRequestSpec(null);
	}

	@Override
	public AgentClientRequestSpec goal(String goal) {
		return goal(new Goal(goal));
	}

	@Override
	public AgentClientRequestSpec goal(Goal goal) {
		return new DefaultAgentClientRequestSpec(goal);
	}

	@Override
	public AgentClientResponse run(String goalText) {
		return goal(goalText).run();
	}

	@Override
	public AgentClientResponse run(String goalText, AgentOptions agentOptions) {
		Goal goal = new Goal(goalText, null, agentOptions);
		return goal(goal).run();
	}

	@Override
	public AgentClient.Builder mutate() {
		return new DefaultAgentClientBuilder(this.agentModel).defaultOptions(this.defaultOptions);
	}

	/**
	 * Default implementation of AgentClientRequestSpec.
	 */
	private class DefaultAgentClientRequestSpec implements AgentClientRequestSpec {

		private Goal goal;

		private Path workingDirectory;

		public DefaultAgentClientRequestSpec(Goal goal) {
			this.goal = goal; // Can be null for goal() method
			this.workingDirectory = goal != null ? goal.getWorkingDirectory() : null;
		}

		@Override
		public AgentClientRequestSpec goal(String goalContent) {
			this.goal = new Goal(goalContent);
			return this;
		}

		@Override
		public AgentClientRequestSpec workingDirectory(Path workingDirectory) {
			this.workingDirectory = workingDirectory;
			return this;
		}

		@Override
		public AgentClientResponse run() {
			// Ensure we have a goal before proceeding
			if (this.goal == null) {
				throw new IllegalStateException(
						"Goal must be set before running. Use goal(String) or goal(Goal) first.");
			}

			// Convert Goal to AgentTaskRequest (adapter pattern)
			AgentTaskRequest request = createTaskRequest();

			// Execute via model layer
			AgentResponse response = DefaultAgentClient.this.agentModel.call(request);

			// Wrap in client response
			return new AgentClientResponse(response);
		}

		private AgentTaskRequest createTaskRequest() {
			// Use working directory priority: explicit > goal > builder default > current
			// directory
			Path effectiveWorkingDirectory;
			if (this.workingDirectory != null) {
				// Explicit working directory was set on this request
				effectiveWorkingDirectory = this.workingDirectory;
			}
			else if (this.goal.getWorkingDirectory() != null) {
				// Use working directory from goal
				effectiveWorkingDirectory = this.goal.getWorkingDirectory();
			}
			else if (DefaultAgentClient.this.defaultOptions.getWorkingDirectory() != null) {
				// Use default working directory from builder
				effectiveWorkingDirectory = Path.of(DefaultAgentClient.this.defaultOptions.getWorkingDirectory());
			}
			else {
				// Fall back to current working directory
				effectiveWorkingDirectory = Path.of(System.getProperty("user.dir"));
			}

			// Merge options: goal options override builder defaults
			AgentOptions effectiveOptions = mergeOptions(this.goal.getOptions(),
					DefaultAgentClient.this.defaultOptions);

			return new AgentTaskRequest(this.goal.getContent(), effectiveWorkingDirectory, effectiveOptions);
		}

		private AgentOptions mergeOptions(AgentOptions goalOptions, AgentOptions defaultOptions) {
			// If goal has no options, use defaults
			if (goalOptions == null) {
				return defaultOptions;
			}

			// If no defaults, use goal options
			if (defaultOptions == null) {
				return goalOptions;
			}

			// Both exist - goal options take precedence
			// For now, just return goal options since merging complex
			// TODO: Implement proper options merging if needed
			return goalOptions;
		}

	}

}