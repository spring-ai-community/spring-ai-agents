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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.BidirectionalTransport;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MCP server support with real Claude CLI.
 * <p>
 * These tests verify that MCP configuration is properly passed to the CLI and that the
 * session infrastructure supports MCP servers.
 */
class McpIntegrationIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = CLIOptions.MODEL_HAIKU;

	@Test
	@DisplayName("Should accept CLI options with stdio MCP server configuration")
	void shouldAcceptStdioMcpServerConfiguration() throws Exception {
		// Given - Configure an external stdio MCP server (filesystem server)
		// Note: We're not actually calling the MCP tools, just verifying the config
		// passes through
		McpServerConfig.McpStdioServerConfig filesystemServer = new McpServerConfig.McpStdioServerConfig("npx",
				List.of("-y", "@modelcontextprotocol/server-filesystem", "/tmp"), Map.of());

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.mcpServer("fs", filesystemServer)
			.build();

		// Verify the options are configured correctly
		assertThat(options.getMcpServers()).hasSize(1);
		assertThat(options.getMcpServers()).containsKey("fs");
		assertThat(options.getMcpServers().get("fs")).isInstanceOf(McpServerConfig.McpStdioServerConfig.class);

		List<ParsedMessage> allMessages = new ArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);
		AtomicReference<String> resultText = new AtomicReference<>();

		// When - Start a session with MCP config (simple query, no MCP tool usage)
		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			transport.startSession("What is 2+2? Answer with just the number.", options, message -> {
				allMessages.add(message);
				if (message.isRegularMessage()) {
					Message msg = message.asMessage();
					if (msg instanceof ResultMessage result) {
						resultText.set(result.result());
						resultLatch.countDown();
					}
				}
			}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

			// Wait for result
			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);

			// Then - Session should complete successfully even with MCP config
			assertThat(completed).as("Session should complete with MCP configuration").isTrue();
			assertThat(resultText.get()).as("Should receive result").isNotNull();
			assertThat(allMessages).as("Should receive messages").isNotEmpty();
		}
	}

	@Test
	@DisplayName("Should accept CLI options with multiple MCP server types")
	void shouldAcceptMultipleMcpServerTypes() throws Exception {
		// Given - Configure multiple server types
		McpServerConfig.McpStdioServerConfig stdioServer = new McpServerConfig.McpStdioServerConfig("python",
				List.of("server.py"));

		McpServerConfig.McpSseServerConfig sseServer = new McpServerConfig.McpSseServerConfig(
				"http://localhost:3000/sse", Map.of("Authorization", "Bearer test"));

		McpServerConfig.McpHttpServerConfig httpServer = new McpServerConfig.McpHttpServerConfig(
				"http://localhost:3001/mcp");

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.mcpServer("stdio-server", stdioServer)
			.mcpServer("sse-server", sseServer)
			.mcpServer("http-server", httpServer)
			.build();

		// Then - All servers should be configured
		assertThat(options.getMcpServers()).hasSize(3);
		assertThat(options.getMcpServers().get("stdio-server"))
			.isInstanceOf(McpServerConfig.McpStdioServerConfig.class);
		assertThat(options.getMcpServers().get("sse-server")).isInstanceOf(McpServerConfig.McpSseServerConfig.class);
		assertThat(options.getMcpServers().get("http-server")).isInstanceOf(McpServerConfig.McpHttpServerConfig.class);

		// Verify types are correct
		McpServerConfig.McpSseServerConfig retrievedSse = (McpServerConfig.McpSseServerConfig) options.getMcpServers()
			.get("sse-server");
		assertThat(retrievedSse.url()).isEqualTo("http://localhost:3000/sse");
		assertThat(retrievedSse.headers()).containsEntry("Authorization", "Bearer test");
	}

	@Test
	@DisplayName("Should properly format MCP tool names")
	void shouldProperlyFormatMcpToolNames() {
		// Given - Server and tool names
		String serverName = "filesystem";
		String toolName = "read_file";

		// When - Format tool name
		String formattedName = McpToolNaming.formatToolName(serverName, toolName);

		// Then
		assertThat(formattedName).isEqualTo("mcp__filesystem__read_file");

		// And parsing should work
		String[] parsed = McpToolNaming.parseToolName(formattedName);
		assertThat(parsed).isNotNull();
		assertThat(parsed[0]).isEqualTo("filesystem");
		assertThat(parsed[1]).isEqualTo("read_file");
	}

	@Test
	@DisplayName("Should identify MCP tools by naming convention")
	void shouldIdentifyMcpToolsByNamingConvention() {
		// MCP tools follow the convention
		assertThat(McpToolNaming.isMcpToolName("mcp__fs__read_file")).isTrue();
		assertThat(McpToolNaming.isMcpToolName("mcp__github__create_issue")).isTrue();

		// Regular tools do not
		assertThat(McpToolNaming.isMcpToolName("Bash")).isFalse();
		assertThat(McpToolNaming.isMcpToolName("Read")).isFalse();
		assertThat(McpToolNaming.isMcpToolName("Edit")).isFalse();
	}

	@Test
	@DisplayName("Should configure allowed tools with MCP naming convention")
	void shouldConfigureAllowedToolsWithMcpNamingConvention() {
		// Given - MCP server configuration with specific allowed tools
		McpServerConfig.McpStdioServerConfig filesystemServer = new McpServerConfig.McpStdioServerConfig("npx",
				List.of("-y", "@modelcontextprotocol/server-filesystem", "/tmp"), Map.of());

		// Allow only specific MCP tools
		List<String> allowedTools = List.of("mcp__fs__read_file", "mcp__fs__write_file", "mcp__fs__list_directory");

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.mcpServer("fs", filesystemServer)
			.allowedTools(allowedTools)
			.build();

		// Then
		assertThat(options.getAllowedTools()).containsExactlyElementsOf(allowedTools);
		assertThat(options.getMcpServers()).containsKey("fs");
	}

	@Test
	@DisplayName("McpMessageHandler should route messages to registered servers")
	void mcpMessageHandlerShouldRouteMessagesToRegisteredServers() {
		// Given - A message handler (without a real MCP server, just testing routing
		// logic)
		McpMessageHandler handler = new McpMessageHandler();

		// Verify no server registered initially
		assertThat(handler.hasServer("calc")).isFalse();

		// When handling a message for unknown server
		Map<String, Object> message = Map.of("jsonrpc", "2.0", "id", 1, "method", "tools/list", "params", Map.of());
		Map<String, Object> response = handler.handleMcpMessage("unknown-server", message);

		// Then - Should return error for unknown server
		assertThat(response).isNotNull();
		assertThat(response).containsKey("error");
		@SuppressWarnings("unchecked")
		Map<String, Object> error = (Map<String, Object>) response.get("error");
		assertThat(error.get("code")).isEqualTo(-32601);
		assertThat((String) error.get("message")).contains("Unknown MCP server");
	}

	@Test
	@DisplayName("Should extract server and tool names from MCP tool format")
	void shouldExtractServerAndToolNamesFromMcpToolFormat() {
		// Various MCP tool name formats
		assertThat(McpToolNaming.extractServerName("mcp__github__create_issue")).isEqualTo("github");
		assertThat(McpToolNaming.extractToolName("mcp__github__create_issue")).isEqualTo("create_issue");

		assertThat(McpToolNaming.extractServerName("mcp__file-system__read_file")).isEqualTo("file-system");
		assertThat(McpToolNaming.extractToolName("mcp__file-system__read_file")).isEqualTo("read_file");

		// Tool names with embedded underscores should work
		assertThat(McpToolNaming.extractToolName("mcp__server__tool_with_multiple_parts"))
			.isEqualTo("tool_with_multiple_parts");

		// Non-MCP tools return null
		assertThat(McpToolNaming.extractServerName("Bash")).isNull();
		assertThat(McpToolNaming.extractToolName("Edit")).isNull();
	}

}
