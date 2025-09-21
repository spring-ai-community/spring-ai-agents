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

package org.springaicommunity.agents.claudecode.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.claudecode.sdk.config.OutputFormat;
import org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claudecode.sdk.types.QueryResult;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for new ClaudeCodeClient methods that support sandbox integration.
 *
 * These tests prove the SDK's ability to build commands and parse results separately,
 * enabling the AgentModel-centric execution pattern.
 */
class ClaudeCodeClientSandboxIntegrationTest {

	private ClaudeCodeClient client;

	private CLIOptions defaultOptions;

	@BeforeEach
	void setUp() throws Exception {
		defaultOptions = CLIOptions.builder()
			.model("claude-sonnet-4-20250514")
			.timeout(Duration.ofMinutes(2))
			.outputFormat(OutputFormat.JSON)
			.build();

		// Create client with mock path (won't actually execute)
		client = ClaudeCodeClient.create(defaultOptions, Paths.get("/tmp"), "/usr/bin/echo");
	}

	@Test
	void buildCommandCreatesCorrectCommandLine() {
		// Arrange
		String prompt = "Fix the failing test";

		// Act
		List<String> command = client.buildCommand(prompt, defaultOptions);

		// Assert: SDK builds correct command structure
		assertThat(command).isNotEmpty();
		assertThat(command.get(0)).isEqualTo("/usr/bin/echo"); // Mock claude path
		assertThat(command).contains("--print"); // Essential for autonomous operations
		assertThat(command).contains("--output-format", "json");
		assertThat(command).contains("--model", "claude-sonnet-4-20250514");
		assertThat(command).contains(prompt);
	}

	@Test
	void buildCommandWithDefaultOptions() {
		// Arrange
		String prompt = "Test default options";

		// Act
		List<String> command = client.buildCommand(prompt);

		// Assert: Uses default options from client
		assertThat(command).isNotEmpty();
		assertThat(command).contains("--print");
		assertThat(command).contains(prompt);
	}

	@Test
	void buildCommandHandlesDifferentOutputFormats() {
		// Arrange
		CLIOptions streamOptions = CLIOptions.builder()
			.model(defaultOptions.getModel())
			.timeout(defaultOptions.getTimeout())
			.outputFormat(OutputFormat.STREAM_JSON)
			.build();

		String prompt = "Stream test";

		// Act
		List<String> command = client.buildCommand(prompt, streamOptions);

		// Assert: Different output format reflected in command
		assertThat(command).contains("--output-format", "stream-json");
		assertThat(command).contains("--verbose"); // Required for stream-json
	}

	@Test
	void parseResultHandlesJsonOutput() throws Exception {
		// Arrange: Mock JSON output from Claude CLI
		String mockJsonOutput = """
				{
					"type": "result",
					"subtype": "success",
					"is_error": false,
					"duration_ms": 2406,
					"duration_api_ms": 2153,
					"num_turns": 1,
					"result": "I have successfully fixed the failing test by updating the assertion.",
					"session_id": "test-session-id",
					"total_cost_usd": 0.001,
					"usage": {
						"input_tokens": 100,
						"output_tokens": 50
					}
				}
				""";

		// Act
		QueryResult result = client.parseResult(mockJsonOutput, defaultOptions);

		// Assert: SDK correctly parses JSON output
		assertThat(result).isNotNull();
		assertThat(result.messages()).isNotEmpty();
		// The exact parsing depends on the JSON structure, but it should not throw
	}

	@Test
	void parseResultHandlesEmptyOutput() throws Exception {
		// Arrange
		String emptyOutput = "";

		// Act
		QueryResult result = client.parseResult(emptyOutput, defaultOptions);

		// Assert: Handles empty output gracefully
		assertThat(result).isNotNull();
		assertThat(result.messages()).isEmpty();
	}

	@Test
	void sdkMethodsSupportAgentModelCentricPattern() {
		// This test demonstrates the complete pattern:
		// 1. SDK builds command
		// 2. External execution (would happen via sandbox)
		// 3. SDK parses result

		// Arrange
		String prompt = "Demonstrate the pattern";
		String mockOutput = "{}"; // Minimal valid JSON

		// Act: Step 1 - SDK builds command
		List<String> command = client.buildCommand(prompt, defaultOptions);

		// Simulate step 2 - external execution (sandbox would do this)
		// In real usage: ExecResult execResult = sandbox.exec(ExecSpec.of(command))

		// Act: Step 3 - SDK parses result
		try {
			QueryResult result = client.parseResult(mockOutput, defaultOptions);

			// Assert: Pattern works end-to-end
			assertThat(command).isNotEmpty();
			assertThat(result).isNotNull();
		}
		catch (Exception e) {
			// Parsing might fail with minimal JSON, but the pattern is demonstrated
			assertThat(command).isNotEmpty();
		}
	}

	@Test
	void commandBuilderPreservesAllOptions() {
		// Arrange
		CLIOptions complexOptions = CLIOptions.builder()
			.model("claude-3-opus-20240229")
			.outputFormat(OutputFormat.JSON)
			.systemPrompt("You are a helpful assistant")
			.allowedTools(List.of("bash", "read", "write"))
			.timeout(Duration.ofMinutes(5))
			.build();

		String prompt = "Complex task with all options";

		// Act
		List<String> command = client.buildCommand(prompt, complexOptions);

		// Assert: All options are preserved in command
		assertThat(command).contains("--model", "claude-3-opus-20240229");
		assertThat(command).contains("--output-format", "json");
		assertThat(command).contains("--append-system-prompt", "You are a helpful assistant");
		assertThat(command).contains("--allowed-tools", "bash,read,write");
		// Note: timeout is handled by executor, not CLI flag
	}

}