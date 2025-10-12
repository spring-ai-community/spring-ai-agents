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

package org.springaicommunity.agents.claude.sdk.config;

import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive SDK configuration with builder pattern. Corresponds to ClaudeCodeOptions
 * in Python SDK with Java-specific enhancements.
 */
public record SDKConfiguration(String model, String systemPrompt, String appendSystemPrompt, Integer maxTokens,
		Integer maxThinkingTokens, Duration timeout, Path workingDirectory, List<String> allowedTools,
		List<String> disallowedTools, Map<String, McpServerConfig> mcpServers, boolean strictMcpConfig,
		PermissionMode permissionMode, boolean continueConversation, String resumeFromSession, Integer maxTurns,
		Map<String, Object> additionalSettings) {

	public SDKConfiguration {
		// Validation and defaults
		if (timeout == null) {
			timeout = Duration.ofMinutes(2);
		}
		if (workingDirectory == null) {
			workingDirectory = Paths.get(System.getProperty("user.dir"));
		}
		if (allowedTools == null) {
			allowedTools = List.of();
		}
		if (disallowedTools == null) {
			disallowedTools = List.of();
		}
		if (mcpServers == null) {
			mcpServers = Map.of();
		}
		else {
			mcpServers = Map.copyOf(mcpServers);
		}
		if (permissionMode == null) {
			permissionMode = PermissionMode.DEFAULT;
		}
		if (additionalSettings == null) {
			additionalSettings = Map.of();
		}
		if (maxThinkingTokens == null) {
			maxThinkingTokens = 8000;
		}
	}

	/**
	 * Converts to CLIOptions for transport layer.
	 */
	public CLIOptions toCliOptions() {
		return CLIOptions.builder()
			.model(model)
			.systemPrompt(buildSystemPrompt())
			.maxTokens(maxTokens)
			.timeout(timeout)
			.allowedTools(allowedTools)
			.disallowedTools(disallowedTools)
			.mcpServers(mcpServers)
			.strictMcpConfig(strictMcpConfig)
			.build();
	}

	private String buildSystemPrompt() {
		if (systemPrompt == null && appendSystemPrompt == null) {
			return null;
		}

		if (systemPrompt == null) {
			return appendSystemPrompt;
		}

		if (appendSystemPrompt == null) {
			return systemPrompt;
		}

		return systemPrompt + "\n\n" + appendSystemPrompt;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static SDKConfiguration defaultConfiguration() {
		return new SDKConfiguration(null, null, null, null, 8000, Duration.ofMinutes(2),
				Paths.get(System.getProperty("user.dir")), List.of(), List.of(), Map.of(), false,
				PermissionMode.BYPASS_PERMISSIONS, false, null, null, Map.of());
	}

	// Convenience getters
	public Duration getTimeout() {
		return timeout;
	}

	public Path getWorkingDirectory() {
		return workingDirectory;
	}

	public String getModel() {
		return model;
	}

	public String getSystemPrompt() {
		return buildSystemPrompt();
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public Integer getMaxThinkingTokens() {
		return maxThinkingTokens;
	}

	public List<String> getAllowedTools() {
		return allowedTools;
	}

	public List<String> getDisallowedTools() {
		return disallowedTools;
	}

	public Map<String, McpServerConfig> getMcpServers() {
		return mcpServers;
	}

	public PermissionMode getPermissionMode() {
		return permissionMode;
	}

	public boolean isContinueConversation() {
		return continueConversation;
	}

	public String getResumeFromSession() {
		return resumeFromSession;
	}

	public Integer getMaxTurns() {
		return maxTurns;
	}

	public Map<String, Object> getAdditionalSettings() {
		return additionalSettings;
	}

	public static class Builder {

		private String model;

		private String systemPrompt;

		private String appendSystemPrompt;

		private Integer maxTokens;

		private Integer maxThinkingTokens = 8000;

		private Duration timeout = Duration.ofMinutes(2);

		private Path workingDirectory = Paths.get(System.getProperty("user.dir"));

		private List<String> allowedTools = List.of();

		private List<String> disallowedTools = List.of();

		private Map<String, McpServerConfig> mcpServers = Map.of();

		private boolean strictMcpConfig = false;

		private PermissionMode permissionMode = PermissionMode.BYPASS_PERMISSIONS;

		private boolean continueConversation = false;

		private String resumeFromSession;

		private Integer maxTurns;

		private Map<String, Object> additionalSettings = Map.of();

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public Builder appendSystemPrompt(String appendSystemPrompt) {
			this.appendSystemPrompt = appendSystemPrompt;
			return this;
		}

		public Builder maxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		public Builder maxThinkingTokens(Integer maxThinkingTokens) {
			this.maxThinkingTokens = maxThinkingTokens;
			return this;
		}

		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder workingDirectory(Path workingDirectory) {
			this.workingDirectory = workingDirectory;
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

		public Builder mcpServers(Map<String, McpServerConfig> mcpServers) {
			this.mcpServers = mcpServers != null ? Map.copyOf(mcpServers) : Map.of();
			return this;
		}

		public Builder addMcpServer(String name, McpServerConfig config) {
			Map<String, McpServerConfig> updated = new LinkedHashMap<>(this.mcpServers);
			updated.put(name, config);
			this.mcpServers = Map.copyOf(updated);
			return this;
		}

		public Builder strictMcpConfig(boolean strictMcpConfig) {
			this.strictMcpConfig = strictMcpConfig;
			return this;
		}

		public Builder permissionMode(PermissionMode permissionMode) {
			this.permissionMode = permissionMode;
			return this;
		}

		public Builder continueConversation(boolean continueConversation) {
			this.continueConversation = continueConversation;
			return this;
		}

		public Builder resumeFromSession(String resumeFromSession) {
			this.resumeFromSession = resumeFromSession;
			return this;
		}

		public Builder maxTurns(Integer maxTurns) {
			this.maxTurns = maxTurns;
			return this;
		}

		public Builder additionalSettings(Map<String, Object> additionalSettings) {
			this.additionalSettings = additionalSettings != null ? Map.copyOf(additionalSettings) : Map.of();
			return this;
		}

		public SDKConfiguration build() {
			return new SDKConfiguration(model, systemPrompt, appendSystemPrompt, maxTokens, maxThinkingTokens, timeout,
					workingDirectory, allowedTools, disallowedTools, mcpServers, strictMcpConfig, permissionMode,
					continueConversation, resumeFromSession, maxTurns, additionalSettings);
		}

	}
}
