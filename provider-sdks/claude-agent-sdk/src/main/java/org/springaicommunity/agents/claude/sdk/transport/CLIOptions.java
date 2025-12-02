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

package org.springaicommunity.agents.claude.sdk.transport;

import org.springaicommunity.agents.claude.sdk.config.OutputFormat;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import java.time.Duration;
import java.util.List;

/**
 * Configuration options for Claude CLI commands. Corresponds to ClaudeAgentOptions in
 * Python SDK.
 */
public record CLIOptions(String model, String systemPrompt, Integer maxTokens, Duration timeout,
		List<String> allowedTools, List<String> disallowedTools, PermissionMode permissionMode, boolean interactive,
		OutputFormat outputFormat, List<String> settingSources, String agents, boolean forkSession,
		boolean includePartialMessages) {

	// ============================================================
	// Model ID Constants
	// ============================================================

	/** Claude Haiku 4.5 - Fast and cost-effective model. */
	public static final String MODEL_HAIKU = "claude-haiku-4-5-20251001";

	/** Claude Sonnet 4.5 - Balanced performance model. */
	public static final String MODEL_SONNET = "claude-sonnet-4-5-20250929";

	/** Claude Opus 4.5 - Most capable model. */
	public static final String MODEL_OPUS = "claude-opus-4-5-20251101";

	public CLIOptions {
		// Validation
		if (timeout == null) {
			timeout = Duration.ofMinutes(2);
		}
		if (allowedTools == null) {
			allowedTools = List.of();
		}
		if (disallowedTools == null) {
			disallowedTools = List.of();
		}
		if (permissionMode == null) {
			permissionMode = PermissionMode.DEFAULT;
		}
		if (outputFormat == null) {
			outputFormat = OutputFormat.JSON; // Default to JSON for non-reactive
		}
		if (settingSources == null) {
			settingSources = List.of(); // Default: no filesystem settings loaded
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static CLIOptions defaultOptions() {
		return new CLIOptions(null, null, null, Duration.ofMinutes(2), List.of(), List.of(),
				PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS, false, OutputFormat.JSON, List.of(), null, false, false);
	}

	// Convenience getters
	public Duration getTimeout() {
		return timeout;
	}

	public String getModel() {
		return model;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public List<String> getAllowedTools() {
		return allowedTools;
	}

	public List<String> getDisallowedTools() {
		return disallowedTools;
	}

	public PermissionMode getPermissionMode() {
		return permissionMode;
	}

	public boolean isInteractive() {
		return interactive;
	}

	public OutputFormat getOutputFormat() {
		return outputFormat;
	}

	public List<String> getSettingSources() {
		return settingSources;
	}

	public String getAgents() {
		return agents;
	}

	public boolean isForkSession() {
		return forkSession;
	}

	public boolean isIncludePartialMessages() {
		return includePartialMessages;
	}

	public static class Builder {

		private String model;

		private String systemPrompt;

		private Integer maxTokens;

		private Duration timeout = Duration.ofMinutes(2);

		private List<String> allowedTools = List.of();

		private List<String> disallowedTools = List.of();

		private PermissionMode permissionMode = PermissionMode.BYPASS_PERMISSIONS;

		private boolean interactive = false;

		private OutputFormat outputFormat = OutputFormat.JSON;

		private List<String> settingSources = List.of();

		private String agents;

		private boolean forkSession = false;

		private boolean includePartialMessages = false;

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public Builder maxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder allowedTools(List<String> allowedTools) {
			this.allowedTools = allowedTools != null ? List.copyOf(allowedTools) : List.of();
			return this;
		}

		public Builder disallowedTools(List<String> disallowedTools) {
			this.disallowedTools = disallowedTools != null ? List.copyOf(disallowedTools) : List.of();
			return this;
		}

		public Builder permissionMode(PermissionMode permissionMode) {
			this.permissionMode = permissionMode;
			return this;
		}

		public Builder interactive(boolean interactive) {
			this.interactive = interactive;
			return this;
		}

		public Builder outputFormat(OutputFormat outputFormat) {
			this.outputFormat = outputFormat;
			return this;
		}

		public Builder settingSources(List<String> settingSources) {
			this.settingSources = settingSources != null ? List.copyOf(settingSources) : List.of();
			return this;
		}

		public Builder agents(String agents) {
			this.agents = agents;
			return this;
		}

		public Builder forkSession(boolean forkSession) {
			this.forkSession = forkSession;
			return this;
		}

		public Builder includePartialMessages(boolean includePartialMessages) {
			this.includePartialMessages = includePartialMessages;
			return this;
		}

		public CLIOptions build() {
			return new CLIOptions(model, systemPrompt, maxTokens, timeout, allowedTools, disallowedTools,
					permissionMode, interactive, outputFormat, settingSources, agents, forkSession,
					includePartialMessages);
		}

	}
}