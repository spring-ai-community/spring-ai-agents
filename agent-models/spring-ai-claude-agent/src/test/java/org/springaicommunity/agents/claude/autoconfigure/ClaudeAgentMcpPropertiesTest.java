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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.claude.agent.sdk.mcp.McpServerConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ClaudeAgentMcpProperties.
 */
class ClaudeAgentMcpPropertiesTest {

	@Nested
	@DisplayName("Default Values Tests")
	class DefaultValuesTests {

		@Test
		@DisplayName("Should have empty servers by default")
		void shouldHaveEmptyServersByDefault() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			assertThat(properties.getServers()).isEmpty();
			assertThat(properties.toMcpServerConfigs()).isEmpty();
		}

	}

	@Nested
	@DisplayName("Stdio Server Tests")
	class StdioServerTests {

		@Test
		@DisplayName("Should convert stdio server properties to config")
		void shouldConvertStdioServerToConfig() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			ClaudeAgentMcpProperties.McpServerProperties serverProps = new ClaudeAgentMcpProperties.McpServerProperties();
			serverProps.setType("stdio");
			serverProps.setCommand("npx");
			serverProps.setArgs(List.of("-y", "@modelcontextprotocol/server-filesystem", "/tmp"));
			serverProps.setEnv(Map.of("DEBUG", "true"));

			properties.getServers().put("filesystem", serverProps);

			Map<String, McpServerConfig> configs = properties.toMcpServerConfigs();

			assertThat(configs).hasSize(1);
			assertThat(configs).containsKey("filesystem");

			McpServerConfig config = configs.get("filesystem");
			assertThat(config).isInstanceOf(McpServerConfig.McpStdioServerConfig.class);

			McpServerConfig.McpStdioServerConfig stdioConfig = (McpServerConfig.McpStdioServerConfig) config;
			assertThat(stdioConfig.command()).isEqualTo("npx");
			assertThat(stdioConfig.args()).containsExactly("-y", "@modelcontextprotocol/server-filesystem", "/tmp");
			assertThat(stdioConfig.env()).containsEntry("DEBUG", "true");
		}

	}

	@Nested
	@DisplayName("SSE Server Tests")
	class SseServerTests {

		@Test
		@DisplayName("Should convert SSE server properties to config")
		void shouldConvertSseServerToConfig() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			ClaudeAgentMcpProperties.McpServerProperties serverProps = new ClaudeAgentMcpProperties.McpServerProperties();
			serverProps.setType("sse");
			serverProps.setUrl("http://localhost:3000/sse");
			serverProps.setHeaders(Map.of("Authorization", "Bearer token123"));

			properties.getServers().put("github", serverProps);

			Map<String, McpServerConfig> configs = properties.toMcpServerConfigs();

			assertThat(configs).hasSize(1);
			assertThat(configs).containsKey("github");

			McpServerConfig config = configs.get("github");
			assertThat(config).isInstanceOf(McpServerConfig.McpSseServerConfig.class);

			McpServerConfig.McpSseServerConfig sseConfig = (McpServerConfig.McpSseServerConfig) config;
			assertThat(sseConfig.url()).isEqualTo("http://localhost:3000/sse");
			assertThat(sseConfig.headers()).containsEntry("Authorization", "Bearer token123");
		}

	}

	@Nested
	@DisplayName("HTTP Server Tests")
	class HttpServerTests {

		@Test
		@DisplayName("Should convert HTTP server properties to config")
		void shouldConvertHttpServerToConfig() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			ClaudeAgentMcpProperties.McpServerProperties serverProps = new ClaudeAgentMcpProperties.McpServerProperties();
			serverProps.setType("http");
			serverProps.setUrl("http://localhost:8080/mcp");

			properties.getServers().put("custom", serverProps);

			Map<String, McpServerConfig> configs = properties.toMcpServerConfigs();

			assertThat(configs).hasSize(1);
			assertThat(configs).containsKey("custom");

			McpServerConfig config = configs.get("custom");
			assertThat(config).isInstanceOf(McpServerConfig.McpHttpServerConfig.class);

			McpServerConfig.McpHttpServerConfig httpConfig = (McpServerConfig.McpHttpServerConfig) config;
			assertThat(httpConfig.url()).isEqualTo("http://localhost:8080/mcp");
		}

	}

	@Nested
	@DisplayName("Multiple Servers Tests")
	class MultipleServersTests {

		@Test
		@DisplayName("Should support multiple server configurations")
		void shouldSupportMultipleServers() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			ClaudeAgentMcpProperties.McpServerProperties stdioProps = new ClaudeAgentMcpProperties.McpServerProperties();
			stdioProps.setType("stdio");
			stdioProps.setCommand("npx");
			stdioProps.setArgs(List.of("-y", "@modelcontextprotocol/server-filesystem"));

			ClaudeAgentMcpProperties.McpServerProperties sseProps = new ClaudeAgentMcpProperties.McpServerProperties();
			sseProps.setType("sse");
			sseProps.setUrl("http://localhost:3000/sse");

			properties.getServers().put("filesystem", stdioProps);
			properties.getServers().put("github", sseProps);

			Map<String, McpServerConfig> configs = properties.toMcpServerConfigs();

			assertThat(configs).hasSize(2);
			assertThat(configs).containsKeys("filesystem", "github");
			assertThat(configs.get("filesystem")).isInstanceOf(McpServerConfig.McpStdioServerConfig.class);
			assertThat(configs.get("github")).isInstanceOf(McpServerConfig.McpSseServerConfig.class);
		}

	}

	@Nested
	@DisplayName("Invalid Configuration Tests")
	class InvalidConfigurationTests {

		@Test
		@DisplayName("Should skip server with null type")
		void shouldSkipServerWithNullType() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			ClaudeAgentMcpProperties.McpServerProperties serverProps = new ClaudeAgentMcpProperties.McpServerProperties();
			// No type set

			properties.getServers().put("invalid", serverProps);

			Map<String, McpServerConfig> configs = properties.toMcpServerConfigs();

			assertThat(configs).isEmpty();
		}

		@Test
		@DisplayName("Should skip server with unknown type")
		void shouldSkipServerWithUnknownType() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			ClaudeAgentMcpProperties.McpServerProperties serverProps = new ClaudeAgentMcpProperties.McpServerProperties();
			serverProps.setType("unknown");

			properties.getServers().put("invalid", serverProps);

			Map<String, McpServerConfig> configs = properties.toMcpServerConfigs();

			assertThat(configs).isEmpty();
		}

	}

	@Nested
	@DisplayName("Case Insensitive Type Tests")
	class CaseInsensitiveTypeTests {

		@Test
		@DisplayName("Should handle uppercase type")
		void shouldHandleUppercaseType() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			ClaudeAgentMcpProperties.McpServerProperties serverProps = new ClaudeAgentMcpProperties.McpServerProperties();
			serverProps.setType("STDIO");
			serverProps.setCommand("npx");

			properties.getServers().put("test", serverProps);

			Map<String, McpServerConfig> configs = properties.toMcpServerConfigs();

			assertThat(configs).hasSize(1);
			assertThat(configs.get("test")).isInstanceOf(McpServerConfig.McpStdioServerConfig.class);
		}

		@Test
		@DisplayName("Should handle mixed case type")
		void shouldHandleMixedCaseType() {
			ClaudeAgentMcpProperties properties = new ClaudeAgentMcpProperties();

			ClaudeAgentMcpProperties.McpServerProperties serverProps = new ClaudeAgentMcpProperties.McpServerProperties();
			serverProps.setType("SsE");
			serverProps.setUrl("http://localhost:3000/sse");

			properties.getServers().put("test", serverProps);

			Map<String, McpServerConfig> configs = properties.toMcpServerConfigs();

			assertThat(configs).hasSize(1);
			assertThat(configs.get("test")).isInstanceOf(McpServerConfig.McpSseServerConfig.class);
		}

	}

}
