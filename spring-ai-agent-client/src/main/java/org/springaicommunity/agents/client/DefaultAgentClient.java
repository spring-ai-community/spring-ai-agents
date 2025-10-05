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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.springaicommunity.agents.client.advisor.AgentModelCallAdvisor;
import org.springaicommunity.agents.client.advisor.DefaultAgentCallAdvisorChain;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
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

	private final List<AgentCallAdvisor> defaultAdvisors;

	/**
	 * Create a new DefaultAgentClient with the given agent model.
	 * @param agentModel the underlying agent model
	 */
	public DefaultAgentClient(AgentModel agentModel) {
		this(agentModel, new DefaultAgentOptions(), new ArrayList<>());
	}

	/**
	 * Create a new DefaultAgentClient with the given agent model and default options.
	 * @param agentModel the underlying agent model
	 * @param defaultOptions default options for all requests
	 */
	public DefaultAgentClient(AgentModel agentModel, AgentOptions defaultOptions) {
		this(agentModel, defaultOptions, new ArrayList<>());
	}

	/**
	 * Create a new DefaultAgentClient with the given agent model, default options, and
	 * advisors.
	 * @param agentModel the underlying agent model
	 * @param defaultOptions default options for all requests
	 * @param defaultAdvisors default advisors for all requests
	 */
	public DefaultAgentClient(AgentModel agentModel, AgentOptions defaultOptions,
			List<AgentCallAdvisor> defaultAdvisors) {
		this.agentModel = Objects.requireNonNull(agentModel, "AgentModel cannot be null");
		this.defaultOptions = defaultOptions != null ? defaultOptions : new DefaultAgentOptions();
		this.defaultAdvisors = defaultAdvisors != null ? new ArrayList<>(defaultAdvisors) : new ArrayList<>();
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
		return new DefaultAgentClientBuilder(this.agentModel).defaultOptions(this.defaultOptions)
			.defaultAdvisors(this.defaultAdvisors);
	}

	/**
	 * Default implementation of AgentClientRequestSpec.
	 */
	private class DefaultAgentClientRequestSpec implements AgentClientRequestSpec {

		private Goal goal;

		private Path workingDirectory;

		private List<AgentCallAdvisor> requestAdvisors = new ArrayList<>();

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
		public AgentClientRequestSpec advisors(AgentCallAdvisor... advisors) {
			this.requestAdvisors.addAll(Arrays.asList(advisors));
			return this;
		}

		@Override
		public AgentClientRequestSpec advisors(List<AgentCallAdvisor> advisors) {
			this.requestAdvisors.addAll(advisors);
			return this;
		}

		@Override
		public AgentClientResponse run() {
			// Ensure we have a goal before proceeding
			if (this.goal == null) {
				throw new IllegalStateException(
						"Goal must be set before running. Use goal(String) or goal(Goal) first.");
			}

			// Determine effective working directory
			Path effectiveWorkingDirectory = determineWorkingDirectory();

			// Merge options
			AgentOptions effectiveOptions = mergeOptions(this.goal.getOptions(),
					DefaultAgentClient.this.defaultOptions);

			// Create client-layer request
			AgentClientRequest request = new AgentClientRequest(this.goal, effectiveWorkingDirectory, effectiveOptions,
					new HashMap<>());

			// Build advisor chain with terminal advisor
			List<AgentCallAdvisor> advisors = new ArrayList<>(DefaultAgentClient.this.defaultAdvisors);
			advisors.addAll(this.requestAdvisors);
			advisors.add(new AgentModelCallAdvisor(DefaultAgentClient.this.agentModel));

			var chain = DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build();

			// Execute through advisor chain
			return chain.nextCall(request);
		}

		private Path determineWorkingDirectory() {
			// Use working directory priority: explicit > goal > builder default > current
			// directory
			if (this.workingDirectory != null) {
				// Explicit working directory was set on this request
				return this.workingDirectory;
			}
			else if (this.goal.getWorkingDirectory() != null) {
				// Use working directory from goal
				return this.goal.getWorkingDirectory();
			}
			else if (DefaultAgentClient.this.defaultOptions.getWorkingDirectory() != null) {
				// Use default working directory from builder
				return Path.of(DefaultAgentClient.this.defaultOptions.getWorkingDirectory());
			}
			else {
				// Fall back to current working directory
				return Path.of(System.getProperty("user.dir"));
			}
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