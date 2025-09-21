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

package org.springaicommunity.agents.claudecode.sdk.transport;

import org.springaicommunity.agents.claudecode.sdk.config.OutputFormat;
import org.springaicommunity.agents.claudecode.sdk.config.PermissionMode;
import java.time.Duration;
import java.util.List;

/**
 * Configuration options for Claude CLI commands. Corresponds to ClaudeCodeOptions in
 * Python SDK.
 */
public record CLIOptions(String model, String systemPrompt, Integer maxTokens, Duration timeout,
		List<String> allowedTools, List<String> disallowedTools, PermissionMode permissionMode, boolean interactive,
		OutputFormat outputFormat) {

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
	}

	public static Builder builder() {
		return new Builder();
	}

	public static CLIOptions defaultOptions() {
		return new CLIOptions(null, null, null, Duration.ofMinutes(2), List.of(), List.of(),
				PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS, false, OutputFormat.JSON);
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

		public CLIOptions build() {
			return new CLIOptions(model, systemPrompt, maxTokens, timeout, allowedTools, disallowedTools,
					permissionMode, interactive, outputFormat);
		}

	}
}