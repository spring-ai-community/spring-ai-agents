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
package org.springaicommunity.agents.claude.autoconfigure;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Claude Code agent model.
 *
 * <p>
 * Allows configuration of Claude Code agent behavior through application properties in
 * the {@code spring.ai.agents.claude-code} namespace.
 *
 * <p>
 * Example configuration:
 *
 * <pre>
 * spring:
 *   ai:
 *     agents:
 *       claude-code:
 *         model: "claude-sonnet-4-5"
 *         timeout: "PT5M"
 *         yolo: true
 *         executable-path: "/usr/local/bin/claude"
 *         max-thinking-tokens: 10000
 *         system-prompt: "You are a helpful coding assistant."
 *         append-system-prompt: "Be concise and focused."
 *         allowed-tools:
 *           - Read
 *           - Write
 *           - Bash
 *         disallowed-tools:
 *           - WebSearch
 *         permission-mode: bypassPermissions
 *         max-turns: 10
 *         max-budget-usd: 0.50
 *         fallback-model: "claude-haiku-3-5-20241022"
 *         # Advanced options for Python SDK parity
 *         add-dirs:
 *           - /workspace/shared-libs
 *           - /workspace/docs
 *         settings: /etc/claude/custom-settings.json
 *         permission-prompt-tool-name: MyPermissionTool
 *         extra-args:
 *           debug-to-stderr: ~
 *           custom-flag: "value"
 *         env:
 *           CUSTOM_API_KEY: "${CUSTOM_API_KEY}"
 *           DEBUG_MODE: "true"
 *         max-buffer-size: 2097152
 *         user: claude-runner
 * </pre>
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.ai.agents.claude-code")
public class ClaudeAgentProperties {

	/**
	 * Claude model to use for agent tasks.
	 */
	private String model = "claude-sonnet-4-5";

	/**
	 * Timeout for agent task execution.
	 */
	private Duration timeout = Duration.ofMinutes(5);

	/**
	 * Whether to enable "yolo" mode (bypass all permission checks).
	 */
	private boolean yolo = true;

	/**
	 * Path to the Claude CLI executable.
	 */
	private String executablePath;

	/**
	 * Maximum thinking tokens for extended thinking mode.
	 */
	private Integer maxThinkingTokens;

	/**
	 * System prompt to use for the agent.
	 */
	private String systemPrompt;

	/**
	 * List of tools that are allowed to be used.
	 */
	private List<String> allowedTools = new ArrayList<>();

	/**
	 * List of tools that are not allowed to be used.
	 */
	private List<String> disallowedTools = new ArrayList<>();

	/**
	 * Permission mode for tool execution. Overrides yolo if set.
	 */
	private String permissionMode;

	/**
	 * JSON schema for structured output.
	 */
	private Map<String, Object> jsonSchema;

	/**
	 * Maximum tokens for the response.
	 */
	private Integer maxTokens;

	/**
	 * Maximum number of agentic turns before stopping.
	 */
	private Integer maxTurns;

	/**
	 * Maximum budget in USD before stopping.
	 */
	private Double maxBudgetUsd;

	/**
	 * Fallback model to use if the primary model is unavailable.
	 */
	private String fallbackModel;

	/**
	 * Additional text to append to the default system prompt. Uses preset mode with
	 * "claude_code" preset.
	 */
	private String appendSystemPrompt;

	// ============================================================
	// Advanced options for full Python SDK parity
	// ============================================================

	/**
	 * Additional directories to include in Claude's context.
	 */
	private List<String> addDirs = new ArrayList<>();

	/**
	 * Custom settings file path.
	 */
	private String settings;

	/**
	 * Permission prompt tool name for interactive permission handling.
	 */
	private String permissionPromptToolName;

	/**
	 * Arbitrary extra CLI arguments. Keys are flag names (without --), values are flag
	 * values (null for boolean flags).
	 */
	private Map<String, String> extraArgs;

	/**
	 * Custom environment variables for the CLI process.
	 */
	private Map<String, String> env;

	/**
	 * Maximum buffer size for JSON parsing in bytes (default 1MB).
	 */
	private Integer maxBufferSize;

	/**
	 * Unix user to run the CLI process as (requires sudo configuration).
	 */
	private String user;

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public boolean isYolo() {
		return yolo;
	}

	public void setYolo(boolean yolo) {
		this.yolo = yolo;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

	public Integer getMaxThinkingTokens() {
		return maxThinkingTokens;
	}

	public void setMaxThinkingTokens(Integer maxThinkingTokens) {
		this.maxThinkingTokens = maxThinkingTokens;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public List<String> getAllowedTools() {
		return allowedTools;
	}

	public void setAllowedTools(List<String> allowedTools) {
		this.allowedTools = allowedTools;
	}

	public List<String> getDisallowedTools() {
		return disallowedTools;
	}

	public void setDisallowedTools(List<String> disallowedTools) {
		this.disallowedTools = disallowedTools;
	}

	public String getPermissionMode() {
		return permissionMode;
	}

	public void setPermissionMode(String permissionMode) {
		this.permissionMode = permissionMode;
	}

	public Map<String, Object> getJsonSchema() {
		return jsonSchema;
	}

	public void setJsonSchema(Map<String, Object> jsonSchema) {
		this.jsonSchema = jsonSchema;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Integer getMaxTurns() {
		return maxTurns;
	}

	public void setMaxTurns(Integer maxTurns) {
		this.maxTurns = maxTurns;
	}

	public Double getMaxBudgetUsd() {
		return maxBudgetUsd;
	}

	public void setMaxBudgetUsd(Double maxBudgetUsd) {
		this.maxBudgetUsd = maxBudgetUsd;
	}

	public String getFallbackModel() {
		return fallbackModel;
	}

	public void setFallbackModel(String fallbackModel) {
		this.fallbackModel = fallbackModel;
	}

	public String getAppendSystemPrompt() {
		return appendSystemPrompt;
	}

	public void setAppendSystemPrompt(String appendSystemPrompt) {
		this.appendSystemPrompt = appendSystemPrompt;
	}

	// ============================================================
	// Getters and Setters for Advanced Options
	// ============================================================

	public List<String> getAddDirs() {
		return addDirs;
	}

	public void setAddDirs(List<String> addDirs) {
		this.addDirs = addDirs;
	}

	public String getSettings() {
		return settings;
	}

	public void setSettings(String settings) {
		this.settings = settings;
	}

	public String getPermissionPromptToolName() {
		return permissionPromptToolName;
	}

	public void setPermissionPromptToolName(String permissionPromptToolName) {
		this.permissionPromptToolName = permissionPromptToolName;
	}

	public Map<String, String> getExtraArgs() {
		return extraArgs;
	}

	public void setExtraArgs(Map<String, String> extraArgs) {
		this.extraArgs = extraArgs;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public Integer getMaxBufferSize() {
		return maxBufferSize;
	}

	public void setMaxBufferSize(Integer maxBufferSize) {
		this.maxBufferSize = maxBufferSize;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Builds CLI options from these properties.
	 * @return configured CLI options
	 */
	public CLIOptions buildCLIOptions() {
		CLIOptions.Builder builder = CLIOptions.builder().model(model).timeout(timeout);

		// Permission mode: explicit setting takes precedence over yolo
		if (permissionMode != null && !permissionMode.isBlank()) {
			builder.permissionMode(PermissionMode.fromValue(permissionMode));
		}
		else if (yolo) {
			builder.permissionMode(PermissionMode.BYPASS_PERMISSIONS);
		}

		// Extended thinking
		if (maxThinkingTokens != null) {
			builder.maxThinkingTokens(maxThinkingTokens);
		}

		// Max tokens
		if (maxTokens != null) {
			builder.maxTokens(maxTokens);
		}

		// System prompt
		if (systemPrompt != null && !systemPrompt.isBlank()) {
			builder.systemPrompt(systemPrompt);
		}

		// Tool filtering
		if (allowedTools != null && !allowedTools.isEmpty()) {
			builder.allowedTools(allowedTools);
		}
		if (disallowedTools != null && !disallowedTools.isEmpty()) {
			builder.disallowedTools(disallowedTools);
		}

		// Structured output
		if (jsonSchema != null && !jsonSchema.isEmpty()) {
			builder.jsonSchema(jsonSchema);
		}

		// Budget control
		if (maxTurns != null) {
			builder.maxTurns(maxTurns);
		}
		if (maxBudgetUsd != null) {
			builder.maxBudgetUsd(maxBudgetUsd);
		}

		// Fallback model
		if (fallbackModel != null && !fallbackModel.isBlank()) {
			builder.fallbackModel(fallbackModel);
		}

		// Append system prompt (uses preset mode)
		if (appendSystemPrompt != null && !appendSystemPrompt.isBlank()) {
			builder.appendSystemPrompt(appendSystemPrompt);
		}

		// ============================================================
		// Advanced options for full Python SDK parity
		// ============================================================

		// Additional context directories
		if (addDirs != null && !addDirs.isEmpty()) {
			builder.addDirs(addDirs.stream().map(Path::of).toList());
		}

		// Custom settings file
		if (settings != null && !settings.isBlank()) {
			builder.settings(settings);
		}

		// Permission prompt tool
		if (permissionPromptToolName != null && !permissionPromptToolName.isBlank()) {
			builder.permissionPromptToolName(permissionPromptToolName);
		}

		// Extra CLI arguments
		if (extraArgs != null && !extraArgs.isEmpty()) {
			builder.extraArgs(extraArgs);
		}

		// Environment variables
		if (env != null && !env.isEmpty()) {
			builder.env(env);
		}

		// Buffer size protection
		if (maxBufferSize != null) {
			builder.maxBufferSize(maxBufferSize);
		}

		// Unix user
		if (user != null && !user.isBlank()) {
			builder.user(user);
		}

		return builder.build();
	}

}