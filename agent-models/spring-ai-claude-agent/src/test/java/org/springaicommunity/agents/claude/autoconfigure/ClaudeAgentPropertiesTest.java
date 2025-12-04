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
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ClaudeAgentProperties.
 */
class ClaudeAgentPropertiesTest {

	@Nested
	@DisplayName("Default Values Tests")
	class DefaultValuesTests {

		@Test
		@DisplayName("Should have correct default values")
		void shouldHaveCorrectDefaultValues() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();

			assertThat(properties.getModel()).isEqualTo("claude-sonnet-4-5");
			assertThat(properties.getTimeout()).isEqualTo(Duration.ofMinutes(5));
			assertThat(properties.isYolo()).isTrue();
			assertThat(properties.getExecutablePath()).isNull();
			assertThat(properties.getMaxThinkingTokens()).isNull();
			assertThat(properties.getSystemPrompt()).isNull();
			assertThat(properties.getAllowedTools()).isEmpty();
			assertThat(properties.getDisallowedTools()).isEmpty();
			assertThat(properties.getPermissionMode()).isNull();
			assertThat(properties.getJsonSchema()).isNull();
			assertThat(properties.getMaxTokens()).isNull();
		}

	}

	@Nested
	@DisplayName("Extended Thinking Tests")
	class ExtendedThinkingTests {

		@Test
		@DisplayName("Should set maxThinkingTokens")
		void shouldSetMaxThinkingTokens() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setMaxThinkingTokens(10000);

			assertThat(properties.getMaxThinkingTokens()).isEqualTo(10000);

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getMaxThinkingTokens()).isEqualTo(10000);
		}

	}

	@Nested
	@DisplayName("System Prompt Tests")
	class SystemPromptTests {

		@Test
		@DisplayName("Should set system prompt")
		void shouldSetSystemPrompt() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setSystemPrompt("You are a helpful coding assistant.");

			assertThat(properties.getSystemPrompt()).isEqualTo("You are a helpful coding assistant.");

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getSystemPrompt()).isEqualTo("You are a helpful coding assistant.");
		}

		@Test
		@DisplayName("Should not set blank system prompt")
		void shouldNotSetBlankSystemPrompt() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setSystemPrompt("   ");

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getSystemPrompt()).isNull();
		}

	}

	@Nested
	@DisplayName("Tool Filtering Tests")
	class ToolFilteringTests {

		@Test
		@DisplayName("Should set allowed tools")
		void shouldSetAllowedTools() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setAllowedTools(List.of("Read", "Write", "Bash"));

			assertThat(properties.getAllowedTools()).containsExactly("Read", "Write", "Bash");

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getAllowedTools()).containsExactly("Read", "Write", "Bash");
		}

		@Test
		@DisplayName("Should set disallowed tools")
		void shouldSetDisallowedTools() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setDisallowedTools(List.of("WebSearch", "Task"));

			assertThat(properties.getDisallowedTools()).containsExactly("WebSearch", "Task");

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getDisallowedTools()).containsExactly("WebSearch", "Task");
		}

	}

	@Nested
	@DisplayName("Permission Mode Tests")
	class PermissionModeTests {

		@Test
		@DisplayName("Should use permission mode when set")
		void shouldUsePermissionModeWhenSet() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setPermissionMode("acceptEdits");

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getPermissionMode()).isEqualTo(PermissionMode.ACCEPT_EDITS);
		}

		@Test
		@DisplayName("Should use bypass when yolo is true and no permission mode")
		void shouldUseBypassWhenYoloIsTrue() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setYolo(true);
			// No explicit permission mode

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getPermissionMode()).isEqualTo(PermissionMode.BYPASS_PERMISSIONS);
		}

		@Test
		@DisplayName("Permission mode should override yolo")
		void permissionModeShouldOverrideYolo() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setYolo(true);
			properties.setPermissionMode("default");

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getPermissionMode()).isEqualTo(PermissionMode.DEFAULT);
		}

	}

	@Nested
	@DisplayName("Structured Output Tests")
	class StructuredOutputTests {

		@Test
		@DisplayName("Should set JSON schema")
		void shouldSetJsonSchema() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			Map<String, Object> schema = Map.of("type", "object", "properties",
					Map.of("answer", Map.of("type", "number")), "required", List.of("answer"));
			properties.setJsonSchema(schema);

			assertThat(properties.getJsonSchema()).isEqualTo(schema);

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getJsonSchema()).isEqualTo(schema);
		}

	}

	@Nested
	@DisplayName("Max Tokens Tests")
	class MaxTokensTests {

		@Test
		@DisplayName("Should set max tokens")
		void shouldSetMaxTokens() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setMaxTokens(4096);

			assertThat(properties.getMaxTokens()).isEqualTo(4096);

			CLIOptions options = properties.buildCLIOptions();
			assertThat(options.getMaxTokens()).isEqualTo(4096);
		}

	}

	@Nested
	@DisplayName("Build CLI Options Tests")
	class BuildCLIOptionsTests {

		@Test
		@DisplayName("Should build CLI options with all properties")
		void shouldBuildCLIOptionsWithAllProperties() {
			ClaudeAgentProperties properties = new ClaudeAgentProperties();
			properties.setModel("claude-opus-4-5");
			properties.setTimeout(Duration.ofMinutes(10));
			properties.setMaxThinkingTokens(5000);
			properties.setMaxTokens(8192);
			properties.setSystemPrompt("Be concise.");
			properties.setAllowedTools(List.of("Read"));
			properties.setDisallowedTools(List.of("WebSearch"));
			properties.setPermissionMode("acceptEdits");
			properties.setJsonSchema(Map.of("type", "object"));

			CLIOptions options = properties.buildCLIOptions();

			assertThat(options.getModel()).isEqualTo("claude-opus-4-5");
			assertThat(options.getTimeout()).isEqualTo(Duration.ofMinutes(10));
			assertThat(options.getMaxThinkingTokens()).isEqualTo(5000);
			assertThat(options.getMaxTokens()).isEqualTo(8192);
			assertThat(options.getSystemPrompt()).isEqualTo("Be concise.");
			assertThat(options.getAllowedTools()).containsExactly("Read");
			assertThat(options.getDisallowedTools()).containsExactly("WebSearch");
			assertThat(options.getPermissionMode()).isEqualTo(PermissionMode.ACCEPT_EDITS);
			assertThat(options.getJsonSchema()).isEqualTo(Map.of("type", "object"));
		}

	}

}
