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

package org.springaicommunity.agents.amazonq;

import org.springaicommunity.agents.model.AgentOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration options for Amazon Q Agent Model implementations.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class AmazonQAgentOptions implements AgentOptions {

	private String model = "amazon-q-developer";

	private Duration timeout = Duration.ofMinutes(10);

	private boolean trustAllTools = true;

	private List<String> trustTools = new ArrayList<>();

	private String agent;

	private boolean verbose = false;

	private String executablePath;

	private String workingDirectory;

	private Map<String, String> environmentVariables = Map.of();

	private Map<String, Object> extras = Map.of();

	private AmazonQAgentOptions() {
	}

	public String getModel() {
		return model;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public boolean isTrustAllTools() {
		return trustAllTools;
	}

	public List<String> getTrustTools() {
		return trustTools;
	}

	public String getAgent() {
		return agent;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	@Override
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	@Override
	public Map<String, Object> getExtras() {
		return extras;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private final AmazonQAgentOptions options = new AmazonQAgentOptions();

		private Builder() {
		}

		public Builder model(String model) {
			options.model = model;
			return this;
		}

		public Builder timeout(Duration timeout) {
			options.timeout = timeout;
			return this;
		}

		public Builder trustAllTools(boolean trustAllTools) {
			options.trustAllTools = trustAllTools;
			if (trustAllTools) {
				options.trustTools = new ArrayList<>(); // Clear selective trust
			}
			return this;
		}

		public Builder trustTools(List<String> trustTools) {
			options.trustTools = trustTools != null ? new ArrayList<>(trustTools) : new ArrayList<>();
			if (trustTools != null && !trustTools.isEmpty()) {
				options.trustAllTools = false; // Disable trust-all with selective
			}
			return this;
		}

		public Builder agent(String agent) {
			options.agent = agent;
			return this;
		}

		public Builder verbose(boolean verbose) {
			options.verbose = verbose;
			return this;
		}

		public Builder executablePath(String executablePath) {
			options.executablePath = executablePath;
			return this;
		}

		public Builder workingDirectory(String workingDirectory) {
			options.workingDirectory = workingDirectory;
			return this;
		}

		public Builder environmentVariables(Map<String, String> environmentVariables) {
			options.environmentVariables = environmentVariables != null ? environmentVariables : Map.of();
			return this;
		}

		public Builder extras(Map<String, Object> extras) {
			options.extras = extras != null ? extras : Map.of();
			return this;
		}

		public AmazonQAgentOptions build() {
			return options;
		}

	}

}
