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
import org.springaicommunity.agents.claude.sdk.mcp.McpServerConfig;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration options for Claude CLI commands. Corresponds to ClaudeAgentOptions in
 * Python SDK.
 */
public record CLIOptions(String model, String systemPrompt, Integer maxTokens, Integer maxThinkingTokens,
		Duration timeout, List<String> allowedTools, List<String> disallowedTools, PermissionMode permissionMode,
		boolean interactive, OutputFormat outputFormat, List<String> settingSources, String agents, boolean forkSession,
		boolean includePartialMessages, Map<String, Object> jsonSchema, Map<String, McpServerConfig> mcpServers) {

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
		if (mcpServers == null) {
			mcpServers = Map.of();
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static CLIOptions defaultOptions() {
		return new CLIOptions(null, null, null, null, Duration.ofMinutes(2), List.of(), List.of(),
				PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS, false, OutputFormat.JSON, List.of(), null, false, false,
				null, Map.of());
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

	public Integer getMaxThinkingTokens() {
		return maxThinkingTokens;
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

	public Map<String, Object> getJsonSchema() {
		return jsonSchema;
	}

	public Map<String, McpServerConfig> getMcpServers() {
		return mcpServers;
	}

	public static class Builder {

		private String model;

		private String systemPrompt;

		private Integer maxTokens;

		private Integer maxThinkingTokens;

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

		private Map<String, Object> jsonSchema;

		private Map<String, McpServerConfig> mcpServers = Map.of();

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

		public Builder maxThinkingTokens(Integer maxThinkingTokens) {
			this.maxThinkingTokens = maxThinkingTokens;
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

		public Builder jsonSchema(Map<String, Object> jsonSchema) {
			this.jsonSchema = jsonSchema != null ? Map.copyOf(jsonSchema) : null;
			return this;
		}

		/**
		 * Sets all MCP servers for this session.
		 * @param mcpServers map of server name to configuration
		 * @return this builder
		 */
		public Builder mcpServers(Map<String, McpServerConfig> mcpServers) {
			this.mcpServers = mcpServers != null ? Map.copyOf(mcpServers) : Map.of();
			return this;
		}

		/**
		 * Adds a single MCP server to this session.
		 * @param name the server name (used in tool naming: mcp__{name}__{tool})
		 * @param config the server configuration
		 * @return this builder
		 */
		public Builder mcpServer(String name, McpServerConfig config) {
			if (this.mcpServers.isEmpty()) {
				this.mcpServers = new HashMap<>();
			}
			else if (!(this.mcpServers instanceof HashMap)) {
				this.mcpServers = new HashMap<>(this.mcpServers);
			}
			this.mcpServers.put(name, config);
			return this;
		}

		public CLIOptions build() {
			return new CLIOptions(model, systemPrompt, maxTokens, maxThinkingTokens, timeout, allowedTools,
					disallowedTools, permissionMode, interactive, outputFormat, settingSources, agents, forkSession,
					includePartialMessages, jsonSchema, mcpServers);
		}

	}
}