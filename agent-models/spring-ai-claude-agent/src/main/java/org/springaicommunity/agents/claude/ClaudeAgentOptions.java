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

package org.springaicommunity.agents.claude;

import org.springaicommunity.agents.claude.sdk.config.McpServerConfig;
import org.springaicommunity.agents.model.AgentOptions;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Configuration options for Claude Code Agent Model implementations.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class ClaudeAgentOptions implements AgentOptions {

	/**
	 * The model name to use (e.g., "claude-sonnet-4-20250514" or "sonnet").
	 */
	private String model;

	/**
	 * Timeout for agent execution.
	 */
	private Duration timeout = Duration.ofMinutes(10);

	/**
	 * Working directory for agent execution. If null, uses system temp directory.
	 */
	private String workingDirectory;

	/**
	 * Environment variables to set for the agent process.
	 */
	private Map<String, String> environmentVariables = Map.of();

	/**
	 * Extra provider-specific options for forward compatibility.
	 */
	private Map<String, Object> extras = Map.of();

	/**
	 * Path to the Claude CLI executable. If null, uses default discovery.
	 */
	private String executablePath;

	/**
	 * YOLO mode - "You Only Live Once" mode that bypasses ALL permission checks. When
	 * true, the agent will execute all operations without any safety prompts including
	 * file modifications, command execution, and potentially destructive operations. USE
	 * WITH EXTREME CAUTION - This is the most dangerous permission mode.
	 */
	private boolean yolo = true;

	/**
	 * System prompt configuration. Can be either a simple string or a preset
	 * configuration. Default is null (no system prompt).
	 */
	private SystemPrompt systemPrompt;

	/**
	 * Setting sources to load (user, project, local). Default is empty (no filesystem
	 * settings loaded). This ensures SDK applications have predictable behavior
	 * independent of local filesystem configurations.
	 */
	private List<SettingSource> settingSources = List.of();

	/**
	 * Programmatic subagent definitions. Allows defining agents inline without filesystem
	 * dependencies.
	 */
	private Map<String, AgentDefinition> agents = Map.of();

	/**
	 * MCP servers that should be registered with the Claude CLI instance.
	 */
	private Map<String, McpServerConfig> mcpServers = Map.of();

	private boolean strictMcpConfig = false;

	/**
	 * When true, resumed sessions will fork to a new session ID rather than continuing
	 * the previous session.
	 */
	private boolean forkSession = false;

	/**
	 * Include partial message events for real-time UI streaming.
	 */
	private boolean includePartialMessages = false;

	public ClaudeAgentOptions() {
	}

	public ClaudeAgentOptions(String model) {
		this.model = model;
	}

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

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(Map<String, String> environmentVariables) {
		this.environmentVariables = environmentVariables != null ? environmentVariables : Map.of();
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

	public boolean isYolo() {
		return yolo;
	}

	public void setYolo(boolean yolo) {
		this.yolo = yolo;
	}

	public SystemPrompt getSystemPrompt() {
		return systemPrompt;
	}

	public void setSystemPrompt(SystemPrompt systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public List<SettingSource> getSettingSources() {
		return settingSources;
	}

	public void setSettingSources(List<SettingSource> settingSources) {
		this.settingSources = settingSources != null ? settingSources : List.of();
	}

	public Map<String, AgentDefinition> getAgents() {
		return agents;
	}

	public void setAgents(Map<String, AgentDefinition> agents) {
		this.agents = agents != null ? agents : Map.of();
	}

	public Map<String, McpServerConfig> getMcpServers() {
		return mcpServers;
	}

	public void setMcpServers(Map<String, McpServerConfig> mcpServers) {
		this.mcpServers = mcpServers != null ? Map.copyOf(mcpServers) : Map.of();
	}

	public boolean isStrictMcpConfig() {
		return strictMcpConfig;
	}

	public void setStrictMcpConfig(boolean strictMcpConfig) {
		this.strictMcpConfig = strictMcpConfig;
	}

	public boolean isForkSession() {
		return forkSession;
	}

	public void setForkSession(boolean forkSession) {
		this.forkSession = forkSession;
	}

	public boolean isIncludePartialMessages() {
		return includePartialMessages;
	}

	public void setIncludePartialMessages(boolean includePartialMessages) {
		this.includePartialMessages = includePartialMessages;
	}

	@Override
	public Map<String, Object> getExtras() {
		return extras;
	}

	public void setExtras(Map<String, Object> extras) {
		this.extras = extras != null ? extras : Map.of();
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for ClaudeAgentOptions.
	 */
	public static final class Builder {

		private final ClaudeAgentOptions options = new ClaudeAgentOptions();

		private Builder() {
		}

		public Builder model(String model) {
			options.setModel(model);
			return this;
		}

		public Builder timeout(Duration timeout) {
			options.setTimeout(timeout);
			return this;
		}

		public Builder workingDirectory(String workingDirectory) {
			options.setWorkingDirectory(workingDirectory);
			return this;
		}

		public Builder environmentVariables(Map<String, String> environmentVariables) {
			options.setEnvironmentVariables(environmentVariables);
			return this;
		}

		public Builder executablePath(String executablePath) {
			options.setExecutablePath(executablePath);
			return this;
		}

		public Builder yolo(boolean yolo) {
			options.setYolo(yolo);
			return this;
		}

		public Builder systemPrompt(SystemPrompt systemPrompt) {
			options.setSystemPrompt(systemPrompt);
			return this;
		}

		public Builder systemPrompt(String prompt) {
			options.setSystemPrompt(SystemPrompt.of(prompt));
			return this;
		}

		public Builder settingSources(List<SettingSource> settingSources) {
			options.setSettingSources(settingSources);
			return this;
		}

		public Builder settingSources(SettingSource... settingSources) {
			options.setSettingSources(List.of(settingSources));
			return this;
		}

		public Builder agents(Map<String, AgentDefinition> agents) {
			options.setAgents(agents);
			return this;
		}

		public Builder mcpServers(Map<String, McpServerConfig> mcpServers) {
			options.setMcpServers(mcpServers);
			return this;
		}

		public Builder addMcpServer(String name, McpServerConfig config) {
			Map<String, McpServerConfig> updated = new LinkedHashMap<>(options.getMcpServers());
			updated.put(name, config);
			options.setMcpServers(updated);
			return this;
		}

		public Builder strictMcpConfig(boolean strictMcpConfig) {
			options.setStrictMcpConfig(strictMcpConfig);
			return this;
		}

		public Builder forkSession(boolean forkSession) {
			options.setForkSession(forkSession);
			return this;
		}

		public Builder includePartialMessages(boolean includePartialMessages) {
			options.setIncludePartialMessages(includePartialMessages);
			return this;
		}

		public Builder extras(Map<String, Object> extras) {
			options.setExtras(extras);
			return this;
		}

		public ClaudeAgentOptions build() {
			return options;
		}

	}

}
