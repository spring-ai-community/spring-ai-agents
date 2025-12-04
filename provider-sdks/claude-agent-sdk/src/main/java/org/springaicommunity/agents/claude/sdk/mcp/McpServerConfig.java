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

package org.springaicommunity.agents.claude.sdk.mcp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.modelcontextprotocol.server.McpSyncServer;

import java.util.List;
import java.util.Map;

/**
 * MCP server configuration matching Python SDK types. Supports: stdio, sse, http, sdk
 * (in-process).
 * <p>
 * External servers (stdio, sse, http) are passed to the Claude CLI via --mcp-config.
 * In-process SDK servers are managed by the Java SDK and communicate via the mcp_message
 * control protocol.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = McpServerConfig.McpStdioServerConfig.class,
		visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = McpServerConfig.McpStdioServerConfig.class, name = "stdio"),
		@JsonSubTypes.Type(value = McpServerConfig.McpSseServerConfig.class, name = "sse"),
		@JsonSubTypes.Type(value = McpServerConfig.McpHttpServerConfig.class, name = "http"),
		@JsonSubTypes.Type(value = McpServerConfig.McpSdkServerConfig.class, name = "sdk") })
public sealed interface McpServerConfig permits McpServerConfig.McpStdioServerConfig,
		McpServerConfig.McpSseServerConfig, McpServerConfig.McpHttpServerConfig, McpServerConfig.McpSdkServerConfig {

	/**
	 * Returns the server type identifier.
	 * @return the type string ("stdio", "sse", "http", or "sdk")
	 */
	String type();

	/**
	 * Returns true if this is an in-process SDK server that requires mcp_message control
	 * protocol handling.
	 * @return true for SDK servers, false for external servers
	 */
	@JsonIgnore
	default boolean isSdkServer() {
		return false;
	}

	/**
	 * Stdio-based MCP server configuration. The server is started as a subprocess with
	 * the specified command and arguments.
	 *
	 * @param command the command to execute (e.g., "npx", "node", "python")
	 * @param args command arguments
	 * @param env environment variables to set for the process
	 */
	record McpStdioServerConfig(@JsonProperty("type") String type, @JsonProperty("command") String command,
			@JsonProperty("args") List<String> args,
			@JsonProperty("env") Map<String, String> env) implements McpServerConfig {

		public McpStdioServerConfig(String command, List<String> args, Map<String, String> env) {
			this("stdio", command, args, env);
		}

		public McpStdioServerConfig(String command, List<String> args) {
			this(command, args, Map.of());
		}

		public McpStdioServerConfig(String command) {
			this(command, List.of(), Map.of());
		}

		@Override
		public String type() {
			return "stdio";
		}

	}

	/**
	 * Server-Sent Events (SSE) based MCP server configuration. Connects to a remote
	 * server via HTTP SSE transport.
	 *
	 * @param url the SSE endpoint URL
	 * @param headers HTTP headers to include in requests
	 */
	record McpSseServerConfig(@JsonProperty("type") String type, @JsonProperty("url") String url,
			@JsonProperty("headers") Map<String, String> headers) implements McpServerConfig {

		public McpSseServerConfig(String url, Map<String, String> headers) {
			this("sse", url, headers);
		}

		public McpSseServerConfig(String url) {
			this(url, Map.of());
		}

		@Override
		public String type() {
			return "sse";
		}

	}

	/**
	 * HTTP-based MCP server configuration. Connects to a remote server via HTTP
	 * transport.
	 *
	 * @param url the HTTP endpoint URL
	 * @param headers HTTP headers to include in requests
	 */
	record McpHttpServerConfig(@JsonProperty("type") String type, @JsonProperty("url") String url,
			@JsonProperty("headers") Map<String, String> headers) implements McpServerConfig {

		public McpHttpServerConfig(String url, Map<String, String> headers) {
			this("http", url, headers);
		}

		public McpHttpServerConfig(String url) {
			this(url, Map.of());
		}

		@Override
		public String type() {
			return "http";
		}

	}

	/**
	 * In-process SDK MCP server configuration. The server is managed by the Java SDK and
	 * communicates via the mcp_message control protocol.
	 * <p>
	 * The instance field is @JsonIgnore because it cannot be serialized to the CLI. Only
	 * the type and name are passed to the CLI; the instance is used internally for
	 * handling mcp_message requests.
	 *
	 * @param name the server name (used in tool naming: mcp__{name}__{tool})
	 * @param instance the MCP server instance (not serialized to CLI)
	 */
	record McpSdkServerConfig(@JsonProperty("type") String type, @JsonProperty("name") String name,
			@JsonIgnore McpSyncServer instance) implements McpServerConfig {

		public McpSdkServerConfig(String name, McpSyncServer instance) {
			this("sdk", name, instance);
		}

		@Override
		public String type() {
			return "sdk";
		}

		@Override
		public boolean isSdkServer() {
			return true;
		}

	}

}
