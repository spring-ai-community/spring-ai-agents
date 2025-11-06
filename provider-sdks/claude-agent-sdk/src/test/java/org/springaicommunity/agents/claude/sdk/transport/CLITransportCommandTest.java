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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.claude.sdk.config.OutputFormat;
import org.springaicommunity.agents.claude.sdk.config.McpServerConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CLITransportCommandTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	void buildCommandIncludesCombinedMcpServers() throws Exception {
		CLITransport transport = new CLITransport(Path.of("."), Duration.ofSeconds(30), "claude");

		Map<String, McpServerConfig> servers = new LinkedHashMap<>();
		servers.put("my-tool",
				McpServerConfig.stdio("python", List.of("./my_mcp_server.py"), Map.of("DEBUG", "${DEBUG:-false}")));
		servers.put("http-service",
				McpServerConfig.http("https://api.example.com/mcp", Map.of("X-API-Key", "${API_KEY}")));

		CLIOptions options = CLIOptions.builder()
			.outputFormat(OutputFormat.JSON)
			.mcpServers(servers)
			.strictMcpConfig(true)
			.build();

		List<String> command = transport.buildCommand("inspect project", options);

		assertThat(command).contains("--mcp-config", "--strict-mcp-config");
		int jsonIndex = command.indexOf("--mcp-config") + 1;
		String payload = command.get(jsonIndex);

		JsonNode node = OBJECT_MAPPER.readTree(payload);
		JsonNode stdioNode = node.path("mcpServers").path("my-tool");
		assertThat(stdioNode.path("command").asText()).isEqualTo("python");
		assertThat(stdioNode.path("args")).isNotNull();
		assertThat(stdioNode.path("env").path("DEBUG").asText()).isEqualTo("${DEBUG:-false}");
		JsonNode httpNode = node.path("mcpServers").path("http-service");
		assertThat(httpNode.path("type").asText()).isEqualTo("http");
		assertThat(httpNode.path("url").asText()).isEqualTo("https://api.example.com/mcp");
		assertThat(httpNode.path("headers").path("X-API-Key").asText()).isEqualTo("${API_KEY}");
		assertThat(command.indexOf("--")).isGreaterThan(jsonIndex);
		assertThat(command.indexOf("--strict-mcp-config")).isLessThan(command.indexOf("--"));
	}

}
