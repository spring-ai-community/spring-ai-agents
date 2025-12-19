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

package org.springaicommunity.agents.claude.sdk.transport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springaicommunity.agents.claude.sdk.config.PluginConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for BidirectionalTransport command building and configuration. Note: Full
 * integration tests with actual CLI are in the integration test suite.
 *
 * <p>
 * Important: In bidirectional mode, the prompt is NOT passed as a command-line argument.
 * Instead, it is sent via stdin as a JSON user message after the process starts. This
 * matches the CLI behavior where --input-format stream-json mode expects input via stdin.
 * </p>
 */
class BidirectionalTransportTest {

	@TempDir
	Path tempDir;

	@Nested
	@DisplayName("Command Building Tests")
	class CommandBuildingTests {

		@Test
		@DisplayName("Should build command with required bidirectional flags")
		void buildCommandWithBidirectionalFlags() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder()
				.model("claude-sonnet-4-20250514")
				.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
				.build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - verify bidirectional flags
			assertThat(command).contains("--input-format", "stream-json");
			assertThat(command).contains("--output-format", "stream-json");
			assertThat(command).contains("--permission-prompt-tool", "stdio");
			assertThat(command).contains("--verbose");
			// Should NOT contain --print in bidirectional mode
			assertThat(command).doesNotContain("--print");
		}

		@Test
		@DisplayName("Should include model when specified")
		void buildCommandWithModel() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().model("claude-3-opus-20240229").build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int modelIndex = command.indexOf("--model");
			assertThat(modelIndex).isGreaterThan(-1);
			assertThat(command.get(modelIndex + 1)).isEqualTo("claude-3-opus-20240229");
		}

		@Test
		@DisplayName("Should include system prompt when specified")
		void buildCommandWithSystemPrompt() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().systemPrompt("You are a helpful assistant").build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int promptIndex = command.indexOf("--append-system-prompt");
			assertThat(promptIndex).isGreaterThan(-1);
			assertThat(command.get(promptIndex + 1)).isEqualTo("You are a helpful assistant");
		}

		@Test
		@DisplayName("Should include allowed tools when specified")
		void buildCommandWithAllowedTools() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().allowedTools(List.of("Bash", "Read", "Write")).build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int toolsIndex = command.indexOf("--allowed-tools");
			assertThat(toolsIndex).isGreaterThan(-1);
			assertThat(command.get(toolsIndex + 1)).isEqualTo("Bash,Read,Write");
		}

		@Test
		@DisplayName("Should include disallowed tools when specified")
		void buildCommandWithDisallowedTools() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().disallowedTools(List.of("WebFetch")).build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int toolsIndex = command.indexOf("--disallowed-tools");
			assertThat(toolsIndex).isGreaterThan(-1);
			assertThat(command.get(toolsIndex + 1)).isEqualTo("WebFetch");
		}

		@Test
		@DisplayName("Should include permission mode")
		void buildCommandWithPermissionMode() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().permissionMode(PermissionMode.BYPASS_PERMISSIONS).build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int modeIndex = command.indexOf("--permission-mode");
			assertThat(modeIndex).isGreaterThan(-1);
			assertThat(command.get(modeIndex + 1)).isEqualTo("bypassPermissions");
		}

		@Test
		@DisplayName("Should NOT include prompt as command-line argument in bidirectional mode")
		void shouldNotIncludePromptAsArgument() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - in bidirectional mode, prompt is sent via stdin, not command line
			// Should not have the -- separator used for positional arguments
			assertThat(command).doesNotContain("--");
			// Command should not contain any user prompt text
			// (old code would have "My test prompt" or similar as last arg after --)
			assertThat(command).noneMatch(arg -> arg.contains("test prompt") || arg.contains("2+2"));
		}

		@Test
		@DisplayName("Should handle empty options gracefully")
		void buildCommandWithEmptyOptions() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - should still have bidirectional flags
			assertThat(command).contains("--input-format");
			assertThat(command).contains("--output-format");
			assertThat(command).contains("--permission-prompt-tool");
			// Should not have empty tool lists
			assertThat(command).doesNotContain("--allowed-tools");
		}

		@Test
		@DisplayName("Should build complete command with all options")
		void buildCompleteCommand() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder()
				.model("claude-sonnet-4-20250514")
				.systemPrompt("Be helpful")
				.allowedTools(List.of("Bash", "Read"))
				.disallowedTools(List.of("Write"))
				.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
				.build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - verify all parts
			assertThat(command.get(0)).isEqualTo("/usr/bin/claude");
			// Bidirectional mode should NOT have --print
			assertThat(command).doesNotContain("--print");
			assertThat(command).contains("--input-format", "stream-json");
			assertThat(command).contains("--output-format", "stream-json");
			assertThat(command).contains("--permission-prompt-tool", "stdio");
			assertThat(command).contains("--model", "claude-sonnet-4-20250514");
			assertThat(command).contains("--append-system-prompt", "Be helpful");
			// No prompt separator or prompt in bidirectional mode
			assertThat(command).doesNotContain("--");
		}

	}

	@Nested
	@DisplayName("Transport State Tests")
	class TransportStateTests {

		@Test
		@DisplayName("Should report not running initially")
		void notRunningInitially() {
			BidirectionalTransport transport = new BidirectionalTransport(tempDir);

			assertThat(transport.isRunning()).isFalse();

			transport.close();
		}

		@Test
		@DisplayName("Should close cleanly when not started")
		void closeWhenNotStarted() {
			BidirectionalTransport transport = new BidirectionalTransport(tempDir);

			// Should not throw
			transport.close();

			assertThat(transport.isRunning()).isFalse();
		}

		@Test
		@DisplayName("Should support custom timeout")
		void customTimeout() {
			Duration customTimeout = Duration.ofMinutes(30);
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, customTimeout);

			// Just verify construction succeeded
			assertThat(transport).isNotNull();

			transport.close();
		}

		@Test
		@DisplayName("Should support custom claude path")
		void customClaudePath() {
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/custom/path/claude");

			List<String> command = transport.buildBidirectionalCommand(CLIOptions.builder().build());

			assertThat(command.get(0)).isEqualTo("/custom/path/claude");

			transport.close();
		}

	}

	@Nested
	@DisplayName("Budget Control and Advanced Options Tests")
	class BudgetControlTests {

		@Test
		@DisplayName("Should include max-turns when specified")
		void buildCommandWithMaxTurns() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().maxTurns(10).build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int turnsIndex = command.indexOf("--max-turns");
			assertThat(turnsIndex).isGreaterThan(-1);
			assertThat(command.get(turnsIndex + 1)).isEqualTo("10");
		}

		@Test
		@DisplayName("Should include max-budget-usd when specified")
		void buildCommandWithMaxBudgetUsd() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().maxBudgetUsd(0.50).build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int budgetIndex = command.indexOf("--max-budget-usd");
			assertThat(budgetIndex).isGreaterThan(-1);
			assertThat(command.get(budgetIndex + 1)).isEqualTo("0.5");
		}

		@Test
		@DisplayName("Should include fallback-model when specified")
		void buildCommandWithFallbackModel() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().fallbackModel("claude-haiku-3-5-20241022").build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int fallbackIndex = command.indexOf("--fallback-model");
			assertThat(fallbackIndex).isGreaterThan(-1);
			assertThat(command.get(fallbackIndex + 1)).isEqualTo("claude-haiku-3-5-20241022");
		}

		@Test
		@DisplayName("Should include append-system-prompt when specified via builder")
		void buildCommandWithAppendSystemPrompt() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().appendSystemPrompt("Be concise and focused.").build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int appendIndex = command.indexOf("--append-system-prompt");
			assertThat(appendIndex).isGreaterThan(-1);
			assertThat(command.get(appendIndex + 1)).isEqualTo("Be concise and focused.");
		}

		@Test
		@DisplayName("Should include all budget control options together")
		void buildCommandWithAllBudgetOptions() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder()
				.model("claude-sonnet-4-5")
				.maxTurns(5)
				.maxBudgetUsd(0.25)
				.fallbackModel("claude-haiku-3-5-20241022")
				.appendSystemPrompt("Be helpful")
				.build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			assertThat(command).contains("--max-turns", "5");
			assertThat(command).contains("--max-budget-usd", "0.25");
			assertThat(command).contains("--fallback-model", "claude-haiku-3-5-20241022");
			assertThat(command).contains("--append-system-prompt", "Be helpful");
		}

	}

	@Nested
	@DisplayName("Bidirectional Mode Verification")
	class BidirectionalModeVerification {

		@Test
		@DisplayName("Command should enable full bidirectional communication")
		void commandEnablesBidirectionalMode() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.defaultOptions();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - these flags together enable bidirectional control protocol:
			// 1. --input-format stream-json: allows writing JSON messages to stdin
			// 2. --output-format stream-json: outputs JSON messages on stdout
			// 3. --permission-prompt-tool stdio: enables control protocol for
			// permissions/hooks
			assertThat(command).containsSubsequence("--input-format", "stream-json");
			assertThat(command).containsSubsequence("--output-format", "stream-json");
			assertThat(command).containsSubsequence("--permission-prompt-tool", "stdio");

			transport.close();
		}

		@Test
		@DisplayName("Should NOT include --print in bidirectional mode")
		void shouldNotIncludePrintInBidirectionalMode() {
			// In bidirectional mode (--input-format stream-json), the CLI waits for
			// input via stdin. Using --print with a command-line prompt would conflict
			// with this mode. Instead, the prompt is sent as a JSON user message.
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");

			List<String> command = transport.buildBidirectionalCommand(CLIOptions.builder().build());

			// --print is NOT used in bidirectional mode
			assertThat(command).doesNotContain("--print");

			transport.close();
		}

		@Test
		@DisplayName("Should always include --verbose for control protocol")
		void alwaysIncludesVerbose() {
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");

			List<String> command = transport.buildBidirectionalCommand(CLIOptions.builder().build());

			// --verbose is required with stream-json to get all message types
			assertThat(command).contains("--verbose");

			transport.close();
		}

	}

	@Nested
	@DisplayName("Advanced Python SDK Parity Options Tests")
	class AdvancedOptionsTests {

		@Test
		@DisplayName("Should include add-dir flags for each directory")
		void buildCommandWithAddDirs() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder()
				.addDirs(List.of(Path.of("/workspace/libs"), Path.of("/workspace/docs")))
				.build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - each directory gets its own --add-dir flag
			int firstIndex = command.indexOf("--add-dir");
			assertThat(firstIndex).isGreaterThan(-1);
			assertThat(command.get(firstIndex + 1)).isEqualTo("/workspace/libs");

			int secondIndex = command.indexOf("--add-dir");
			List<String> afterFirst = command.subList(firstIndex + 2, command.size());
			int secondPos = afterFirst.indexOf("--add-dir");
			assertThat(secondPos).isGreaterThan(-1);
			assertThat(afterFirst.get(secondPos + 1)).isEqualTo("/workspace/docs");

			transport.close();
		}

		@Test
		@DisplayName("Should include settings flag when specified")
		void buildCommandWithSettings() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().settings("/etc/claude/settings.json").build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int settingsIndex = command.indexOf("--settings");
			assertThat(settingsIndex).isGreaterThan(-1);
			assertThat(command.get(settingsIndex + 1)).isEqualTo("/etc/claude/settings.json");

			transport.close();
		}

		@Test
		@DisplayName("Should include permission-prompt-tool-name when specified")
		void buildCommandWithPermissionPromptToolName() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().permissionPromptToolName("custom-tool").build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - custom permission prompt tool replaces default "stdio"
			int toolIndex = command.indexOf("--permission-prompt-tool");
			assertThat(toolIndex).isGreaterThan(-1);
			assertThat(command.get(toolIndex + 1)).isEqualTo("custom-tool");

			transport.close();
		}

		@Test
		@DisplayName("Should include plugin-dir flags for each plugin")
		void buildCommandWithPlugins() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder()
				.plugins(List.of(PluginConfig.local("/opt/plugins/custom"), PluginConfig.local("/home/user/plugins")))
				.build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int pluginIndex = command.indexOf("--plugin-dir");
			assertThat(pluginIndex).isGreaterThan(-1);
			assertThat(command.get(pluginIndex + 1)).isEqualTo("/opt/plugins/custom");

			transport.close();
		}

		@Test
		@DisplayName("Should include extra args with values")
		void buildCommandWithExtraArgsWithValue() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder().extraArgs(Map.of("custom-flag", "custom-value")).build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then
			int flagIndex = command.indexOf("--custom-flag");
			assertThat(flagIndex).isGreaterThan(-1);
			assertThat(command.get(flagIndex + 1)).isEqualTo("custom-value");

			transport.close();
		}

		@Test
		@DisplayName("Should include extra args as boolean flags when value is null")
		void buildCommandWithExtraArgsBooleanFlag() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			// Use HashMap to allow null values
			Map<String, String> extraArgs = new java.util.HashMap<>();
			extraArgs.put("debug-to-stderr", null);
			CLIOptions options = CLIOptions.builder().extraArgs(extraArgs).build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - boolean flag should be present without a value
			assertThat(command).contains("--debug-to-stderr");
			int flagIndex = command.indexOf("--debug-to-stderr");
			// Next element should not be the value (either another flag or end)
			if (flagIndex + 1 < command.size()) {
				String nextElement = command.get(flagIndex + 1);
				assertThat(nextElement).startsWith("--").describedAs("Boolean flag should not have a value");
			}

			transport.close();
		}

		@Test
		@DisplayName("Should store user option in CLIOptions")
		void buildCommandWithUser() {
			// Given - user wrapping is done at startSession time, not in
			// buildBidirectionalCommand
			// The buildBidirectionalCommand only generates CLI flags, the
			// wrapCommandForUser
			// is called separately in startSession(). This test verifies the option is
			// stored.
			CLIOptions options = CLIOptions.builder().user("claude-runner").build();

			// Then
			assertThat(options.getUser()).isEqualTo("claude-runner");
		}

		@Test
		@DisplayName("Should include all advanced options together")
		void buildCommandWithAllAdvancedOptions() {
			// Given
			BidirectionalTransport transport = new BidirectionalTransport(tempDir, Duration.ofMinutes(5),
					"/usr/bin/claude");
			CLIOptions options = CLIOptions.builder()
				.model("claude-sonnet-4-5")
				.addDirs(List.of(Path.of("/workspace")))
				.settings("/etc/claude/settings.json")
				.plugins(List.of(PluginConfig.local("/opt/plugins/custom")))
				.extraArgs(Map.of("custom-flag", "value"))
				.build();

			// When
			List<String> command = transport.buildBidirectionalCommand(options);

			// Then - all options should be present
			assertThat(command).contains("--add-dir", "/workspace");
			assertThat(command).contains("--settings", "/etc/claude/settings.json");
			assertThat(command).contains("--plugin-dir", "/opt/plugins/custom");
			assertThat(command).contains("--custom-flag", "value");

			transport.close();
		}

	}

}
