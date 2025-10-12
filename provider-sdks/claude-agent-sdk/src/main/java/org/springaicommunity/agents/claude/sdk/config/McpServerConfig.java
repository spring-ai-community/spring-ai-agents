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

import java.util.List;
import java.util.Map;

/**
 * Model Context Protocol (MCP) server configuration describing either a streamable HTTP
 * transport or a local STDIO command. This class lives in the Claude SDK package so the
 * SDK can expose MCP configuration without depending on higher-level modules.
 */
public record McpServerConfig(String type, String url, String command, List<String> args, Map<String, String> env,
		Map<String, String> headers) {

	public McpServerConfig {
		if (command != null && command.isBlank()) {
			throw new IllegalArgumentException("MCP server command must not be blank when provided");
		}
		if (type != null && type.isBlank()) {
			throw new IllegalArgumentException("MCP server type must not be blank when provided");
		}
		args = args != null ? List.copyOf(args) : List.of();
		env = env != null ? Map.copyOf(env) : Map.of();
		headers = headers != null ? Map.copyOf(headers) : Map.of();
	}

	public static McpServerConfig http(String url) {
		return http(url, Map.of());
	}

	public static McpServerConfig http(String url, Map<String, String> headers) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("MCP server url must not be null or blank");
		}
		return new McpServerConfig("http", url, null, List.of(), Map.of(), headers);
	}

	public static McpServerConfig stdio(String command, List<String> args, Map<String, String> env) {
		if (command == null || command.isBlank()) {
			throw new IllegalArgumentException("MCP stdio server command must not be null or blank");
		}
		return new McpServerConfig(null, null, command, args, env, Map.of());
	}

}
