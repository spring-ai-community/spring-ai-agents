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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentOptions;

/**
 * Default implementation of {@link AgentClient.Builder}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class DefaultAgentClientBuilder implements AgentClient.Builder {

	private final AgentModel agentModel;

	private AgentOptions defaultOptions;

	private List<AgentCallAdvisor> defaultAdvisors;

	public DefaultAgentClientBuilder(AgentModel agentModel) {
		this.agentModel = Objects.requireNonNull(agentModel, "AgentModel cannot be null");
		this.defaultOptions = new DefaultAgentOptions();
		this.defaultAdvisors = new ArrayList<>();
	}

	@Override
	public AgentClient.Builder defaultOptions(AgentOptions agentOptions) {
		this.defaultOptions = agentOptions != null ? agentOptions : new DefaultAgentOptions();
		return this;
	}

	@Override
	public AgentClient.Builder defaultWorkingDirectory(Path workingDirectory) {
		// Build new options with the working directory
		this.defaultOptions = DefaultAgentOptions.builder()
			.from(this.defaultOptions)
			.workingDirectory(workingDirectory != null ? workingDirectory.toString() : null)
			.build();
		return this;
	}

	@Override
	public AgentClient.Builder defaultTimeout(Duration timeout) {
		// Build new options with the timeout
		this.defaultOptions = DefaultAgentOptions.builder().from(this.defaultOptions).timeout(timeout).build();
		return this;
	}

	@Override
	public AgentClient.Builder defaultAdvisors(List<AgentCallAdvisor> advisors) {
		this.defaultAdvisors = advisors != null ? new ArrayList<>(advisors) : new ArrayList<>();
		return this;
	}

	@Override
	public AgentClient.Builder defaultAdvisor(AgentCallAdvisor advisor) {
		if (advisor != null) {
			this.defaultAdvisors.add(advisor);
		}
		return this;
	}

	@Override
	public AgentClient build() {
		return new DefaultAgentClient(this.agentModel, this.defaultOptions, this.defaultAdvisors);
	}

}