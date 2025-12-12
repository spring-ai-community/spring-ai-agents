/*
 * Copyright 2025 Spring AI Community
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

import org.springaicommunity.claude.agent.sdk.mcp.McpServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for MCP (Model Context Protocol) servers.
 *
 * <p>
 * Allows configuration of MCP servers through application properties in the
 * {@code spring.ai.agents.claude-code.mcp} namespace.
 *
 * <p>
 * Example configuration:
 *
 * <pre>
 * spring:
 *   ai:
 *     agents:
 *       claude-code:
 *         mcp:
 *           servers:
 *             filesystem:
 *               type: stdio
 *               command: npx
 *               args:
 *                 - "-y"
 *                 - "@modelcontextprotocol/server-filesystem"
 *                 - "/tmp"
 *             github:
 *               type: sse
 *               url: http://localhost:3000/sse
 *               headers:
 *                 Authorization: "Bearer ${GITHUB_TOKEN}"
 * </pre>
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.ai.agents.claude-code.mcp")
public class ClaudeAgentMcpProperties {

	/**
	 * Map of MCP server configurations, keyed by server name.
	 */
	private Map<String, McpServerProperties> servers = new HashMap<>();

	public Map<String, McpServerProperties> getServers() {
		return servers;
	}

	public void setServers(Map<String, McpServerProperties> servers) {
		this.servers = servers;
	}

	/**
	 * Converts the configured server properties to SDK McpServerConfig objects.
	 * @return map of server name to McpServerConfig
	 */
	public Map<String, McpServerConfig> toMcpServerConfigs() {
		Map<String, McpServerConfig> configs = new HashMap<>();
		for (Map.Entry<String, McpServerProperties> entry : servers.entrySet()) {
			McpServerConfig config = entry.getValue().toMcpServerConfig();
			if (config != null) {
				configs.put(entry.getKey(), config);
			}
		}
		return configs;
	}

	/**
	 * Properties for a single MCP server configuration.
	 */
	public static class McpServerProperties {

		/**
		 * Type of MCP server: stdio, sse, or http.
		 */
		private String type;

		/**
		 * Command to execute (for stdio type).
		 */
		private String command;

		/**
		 * Command arguments (for stdio type).
		 */
		private List<String> args = new ArrayList<>();

		/**
		 * Environment variables (for stdio type).
		 */
		private Map<String, String> env = new HashMap<>();

		/**
		 * Server URL (for sse and http types).
		 */
		private String url;

		/**
		 * HTTP headers (for sse and http types).
		 */
		private Map<String, String> headers = new HashMap<>();

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		public List<String> getArgs() {
			return args;
		}

		public void setArgs(List<String> args) {
			this.args = args;
		}

		public Map<String, String> getEnv() {
			return env;
		}

		public void setEnv(Map<String, String> env) {
			this.env = env;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

		public void setHeaders(Map<String, String> headers) {
			this.headers = headers;
		}

		/**
		 * Converts this properties object to an McpServerConfig.
		 * @return the corresponding McpServerConfig, or null if type is invalid
		 */
		public McpServerConfig toMcpServerConfig() {
			if (type == null) {
				return null;
			}

			return switch (type.toLowerCase()) {
				case "stdio" -> new McpServerConfig.McpStdioServerConfig(command, args, env);
				case "sse" -> new McpServerConfig.McpSseServerConfig(url, headers);
				case "http" -> new McpServerConfig.McpHttpServerConfig(url);
				default -> null;
			};
		}

	}

}
