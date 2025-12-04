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
 *         allowed-tools:
 *           - Read
 *           - Write
 *           - Bash
 *         disallowed-tools:
 *           - WebSearch
 *         permission-mode: bypassPermissions
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

		return builder.build();
	}

}