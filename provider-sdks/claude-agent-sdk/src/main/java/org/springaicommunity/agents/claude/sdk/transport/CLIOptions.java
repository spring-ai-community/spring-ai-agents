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
import org.springaicommunity.agents.claude.sdk.config.PluginConfig;
import org.springaicommunity.agents.claude.sdk.mcp.McpServerConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
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
		boolean includePartialMessages, Map<String, Object> jsonSchema, Map<String, McpServerConfig> mcpServers,
		Integer maxTurns, Double maxBudgetUsd, String fallbackModel, String appendSystemPrompt,
		// Advanced options for full Python SDK parity
		List<Path> addDirs, String settings, String permissionPromptToolName, Map<String, String> extraArgs,
		List<PluginConfig> plugins, Map<String, String> env, Integer maxBufferSize, String user,
		StderrHandler stderrHandler, ToolPermissionCallback toolPermissionCallback) {

	/** Default maximum buffer size for JSON parsing (1MB). */
	public static final int DEFAULT_MAX_BUFFER_SIZE = 1024 * 1024;

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
		// Advanced options defaults
		if (addDirs == null) {
			addDirs = List.of();
		}
		if (extraArgs == null) {
			extraArgs = Map.of();
		}
		if (plugins == null) {
			plugins = List.of();
		}
		if (env == null) {
			env = Map.of();
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static CLIOptions defaultOptions() {
		return new CLIOptions(null, null, null, null, Duration.ofMinutes(2), List.of(), List.of(),
				PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS, false, OutputFormat.JSON, List.of(), null, false, false,
				null, Map.of(), null, null, null, null, List.of(), null, null, Map.of(), List.of(), Map.of(), null,
				null, null, null);
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

	public Integer getMaxTurns() {
		return maxTurns;
	}

	public Double getMaxBudgetUsd() {
		return maxBudgetUsd;
	}

	public String getFallbackModel() {
		return fallbackModel;
	}

	public String getAppendSystemPrompt() {
		return appendSystemPrompt;
	}

	// Advanced options getters
	public List<Path> getAddDirs() {
		return addDirs;
	}

	public String getSettings() {
		return settings;
	}

	public String getPermissionPromptToolName() {
		return permissionPromptToolName;
	}

	public Map<String, String> getExtraArgs() {
		return extraArgs;
	}

	public List<PluginConfig> getPlugins() {
		return plugins;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public Integer getMaxBufferSize() {
		return maxBufferSize;
	}

	public int getEffectiveMaxBufferSize() {
		return maxBufferSize != null ? maxBufferSize : DEFAULT_MAX_BUFFER_SIZE;
	}

	public String getUser() {
		return user;
	}

	public StderrHandler getStderrHandler() {
		return stderrHandler;
	}

	public ToolPermissionCallback getToolPermissionCallback() {
		return toolPermissionCallback;
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

		private Integer maxTurns;

		private Double maxBudgetUsd;

		private String fallbackModel;

		private String appendSystemPrompt;

		// Advanced options for full Python SDK parity
		private List<Path> addDirs = List.of();

		private String settings;

		private String permissionPromptToolName;

		private Map<String, String> extraArgs = Map.of();

		private List<PluginConfig> plugins = List.of();

		private Map<String, String> env = Map.of();

		private Integer maxBufferSize;

		private String user;

		private StderrHandler stderrHandler;

		private ToolPermissionCallback toolPermissionCallback;

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

		/**
		 * Sets the maximum number of agentic turns for this session.
		 * @param maxTurns maximum turns before stopping
		 * @return this builder
		 */
		public Builder maxTurns(Integer maxTurns) {
			this.maxTurns = maxTurns;
			return this;
		}

		/**
		 * Sets the maximum budget in USD for this session.
		 * @param maxBudgetUsd maximum cost before stopping
		 * @return this builder
		 */
		public Builder maxBudgetUsd(Double maxBudgetUsd) {
			this.maxBudgetUsd = maxBudgetUsd;
			return this;
		}

		/**
		 * Sets the fallback model to use if the primary model is unavailable.
		 * @param fallbackModel the fallback model ID
		 * @return this builder
		 */
		public Builder fallbackModel(String fallbackModel) {
			this.fallbackModel = fallbackModel;
			return this;
		}

		/**
		 * Sets additional text to append to the system prompt (uses preset with append).
		 * @param appendSystemPrompt text to append to the default system prompt
		 * @return this builder
		 */
		public Builder appendSystemPrompt(String appendSystemPrompt) {
			this.appendSystemPrompt = appendSystemPrompt;
			return this;
		}

		// ============================================================
		// Advanced options for full Python SDK parity
		// ============================================================

		/**
		 * Sets additional directories to include in Claude's context.
		 * @param addDirs list of directory paths to add
		 * @return this builder
		 */
		public Builder addDirs(List<Path> addDirs) {
			this.addDirs = addDirs != null ? List.copyOf(addDirs) : List.of();
			return this;
		}

		/**
		 * Adds a single directory to Claude's context.
		 * @param dir directory path to add
		 * @return this builder
		 */
		public Builder addDir(Path dir) {
			if (this.addDirs.isEmpty()) {
				this.addDirs = new ArrayList<>();
			}
			else if (!(this.addDirs instanceof ArrayList)) {
				this.addDirs = new ArrayList<>(this.addDirs);
			}
			this.addDirs.add(dir);
			return this;
		}

		/**
		 * Sets a custom settings file path.
		 * @param settings path to the settings file
		 * @return this builder
		 */
		public Builder settings(String settings) {
			this.settings = settings;
			return this;
		}

		/**
		 * Sets the permission prompt tool name for interactive permission handling.
		 * @param permissionPromptToolName the tool name (e.g., "stdio")
		 * @return this builder
		 */
		public Builder permissionPromptToolName(String permissionPromptToolName) {
			this.permissionPromptToolName = permissionPromptToolName;
			return this;
		}

		/**
		 * Sets arbitrary extra CLI arguments.
		 * @param extraArgs map of flag name to value (null value for boolean flags)
		 * @return this builder
		 */
		public Builder extraArgs(Map<String, String> extraArgs) {
			// Note: Cannot use Map.copyOf() because it doesn't allow null values,
			// but null values are used for boolean flags (--flag without value)
			this.extraArgs = extraArgs != null ? new HashMap<>(extraArgs) : Map.of();
			return this;
		}

		/**
		 * Adds a single extra CLI argument.
		 * @param flag the flag name (without --)
		 * @param value the flag value (null for boolean flags)
		 * @return this builder
		 */
		public Builder extraArg(String flag, String value) {
			if (this.extraArgs.isEmpty()) {
				this.extraArgs = new HashMap<>();
			}
			else if (!(this.extraArgs instanceof HashMap)) {
				this.extraArgs = new HashMap<>(this.extraArgs);
			}
			this.extraArgs.put(flag, value);
			return this;
		}

		/**
		 * Sets plugin configurations.
		 * @param plugins list of plugin configs
		 * @return this builder
		 */
		public Builder plugins(List<PluginConfig> plugins) {
			this.plugins = plugins != null ? List.copyOf(plugins) : List.of();
			return this;
		}

		/**
		 * Adds a single plugin.
		 * @param plugin the plugin configuration
		 * @return this builder
		 */
		public Builder plugin(PluginConfig plugin) {
			if (this.plugins.isEmpty()) {
				this.plugins = new ArrayList<>();
			}
			else if (!(this.plugins instanceof ArrayList)) {
				this.plugins = new ArrayList<>(this.plugins);
			}
			this.plugins.add(plugin);
			return this;
		}

		/**
		 * Sets custom environment variables for the CLI process.
		 * @param env map of environment variable name to value
		 * @return this builder
		 */
		public Builder env(Map<String, String> env) {
			this.env = env != null ? Map.copyOf(env) : Map.of();
			return this;
		}

		/**
		 * Adds a single environment variable.
		 * @param name the environment variable name
		 * @param value the environment variable value
		 * @return this builder
		 */
		public Builder env(String name, String value) {
			if (this.env.isEmpty()) {
				this.env = new HashMap<>();
			}
			else if (!(this.env instanceof HashMap)) {
				this.env = new HashMap<>(this.env);
			}
			this.env.put(name, value);
			return this;
		}

		/**
		 * Sets the maximum buffer size for JSON parsing.
		 * @param maxBufferSize maximum bytes (default 1MB)
		 * @return this builder
		 */
		public Builder maxBufferSize(Integer maxBufferSize) {
			this.maxBufferSize = maxBufferSize;
			return this;
		}

		/**
		 * Sets the Unix user to run the CLI process as.
		 * @param user the Unix username (requires sudo configuration)
		 * @return this builder
		 */
		public Builder user(String user) {
			this.user = user;
			return this;
		}

		/**
		 * Sets the stderr handler for capturing CLI diagnostic output.
		 * @param stderrHandler handler for stderr lines
		 * @return this builder
		 */
		public Builder stderrHandler(StderrHandler stderrHandler) {
			this.stderrHandler = stderrHandler;
			return this;
		}

		/**
		 * Sets the tool permission callback for dynamic permission decisions.
		 * @param toolPermissionCallback callback for tool permission checks
		 * @return this builder
		 */
		public Builder toolPermissionCallback(ToolPermissionCallback toolPermissionCallback) {
			this.toolPermissionCallback = toolPermissionCallback;
			return this;
		}

		public CLIOptions build() {
			return new CLIOptions(model, systemPrompt, maxTokens, maxThinkingTokens, timeout, allowedTools,
					disallowedTools, permissionMode, interactive, outputFormat, settingSources, agents, forkSession,
					includePartialMessages, jsonSchema, mcpServers, maxTurns, maxBudgetUsd, fallbackModel,
					appendSystemPrompt, addDirs, settings, permissionPromptToolName, extraArgs, plugins, env,
					maxBufferSize, user, stderrHandler, toolPermissionCallback);
		}

	}
}