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

package org.springaicommunity.agents.amazonqsdk.types;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration options for Amazon Q CLI execution.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class ExecuteOptions {

	private final String model;

	private final Duration timeout;

	private final boolean trustAllTools;

	private final List<String> trustTools;

	private final boolean noInteractive;

	private final String agent;

	private final boolean resume;

	private final boolean verbose;

	private ExecuteOptions(Builder builder) {
		this.model = builder.model;
		this.timeout = builder.timeout;
		this.trustAllTools = builder.trustAllTools;
		this.trustTools = builder.trustTools != null ? new ArrayList<>(builder.trustTools) : new ArrayList<>();
		this.noInteractive = builder.noInteractive;
		this.agent = builder.agent;
		this.resume = builder.resume;
		this.verbose = builder.verbose;
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

	public boolean isNoInteractive() {
		return noInteractive;
	}

	public String getAgent() {
		return agent;
	}

	public boolean isResume() {
		return resume;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private String model;

		private Duration timeout = Duration.ofMinutes(10);

		private boolean trustAllTools = true; // Default to autonomous mode

		private List<String> trustTools;

		private boolean noInteractive = true; // Default to non-interactive

		private String agent;

		private boolean resume = false;

		private boolean verbose = false;

		private Builder() {
		}

		/**
		 * Set the model to use.
		 * @param model the model name
		 * @return this builder
		 */
		public Builder model(String model) {
			this.model = model;
			return this;
		}

		/**
		 * Set the execution timeout.
		 * @param timeout the timeout duration
		 * @return this builder
		 */
		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * Enable trust-all-tools mode (autonomous execution).
		 * @param trustAllTools true to allow all tools without confirmation
		 * @return this builder
		 */
		public Builder trustAllTools(boolean trustAllTools) {
			this.trustAllTools = trustAllTools;
			if (trustAllTools) {
				this.trustTools = null; // Clear selective trust list
			}
			return this;
		}

		/**
		 * Set specific tools to trust.
		 * @param tools list of tool names to trust
		 * @return this builder
		 */
		public Builder trustTools(List<String> tools) {
			this.trustTools = tools;
			if (tools != null && !tools.isEmpty()) {
				this.trustAllTools = false; // Disable trust-all when using selective
				// trust
			}
			return this;
		}

		/**
		 * Enable non-interactive mode.
		 * @param noInteractive true for non-interactive execution
		 * @return this builder
		 */
		public Builder noInteractive(boolean noInteractive) {
			this.noInteractive = noInteractive;
			return this;
		}

		/**
		 * Set the agent (context profile) to use.
		 * @param agent the agent name
		 * @return this builder
		 */
		public Builder agent(String agent) {
			this.agent = agent;
			return this;
		}

		/**
		 * Enable resume mode to continue previous conversation.
		 * @param resume true to resume previous conversation
		 * @return this builder
		 */
		public Builder resume(boolean resume) {
			this.resume = resume;
			return this;
		}

		/**
		 * Enable verbose logging.
		 * @param verbose true to enable verbose mode
		 * @return this builder
		 */
		public Builder verbose(boolean verbose) {
			this.verbose = verbose;
			return this;
		}

		public ExecuteOptions build() {
			return new ExecuteOptions(this);
		}

	}

}
