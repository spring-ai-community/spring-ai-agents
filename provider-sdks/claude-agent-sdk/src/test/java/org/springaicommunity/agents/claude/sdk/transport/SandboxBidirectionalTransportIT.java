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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springaicommunity.sandbox.ExecSpec;
import org.springaicommunity.sandbox.Sandbox;
import org.springaicommunity.claude.agent.sdk.transport.CLIOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for SandboxBidirectionalTransport.
 *
 * <p>
 * Tests verify:
 * </p>
 * <ul>
 * <li>Sandbox integration: process creation is delegated to
 * Sandbox.startInteractive()</li>
 * <li>Direct execution: falls back to ProcessBuilder when no sandbox is configured</li>
 * <li>State machine: proper lifecycle management</li>
 * <li>Command building: correct CLI flags for bidirectional mode</li>
 * </ul>
 *
 * @author Spring AI Community
 */
class SandboxBidirectionalTransportIT {

	private static final Path TEST_WORKING_DIR = Paths.get(System.getProperty("user.dir"));

	private SandboxBidirectionalTransport transport;

	@AfterEach
	void tearDown() {
		if (transport != null) {
			transport.close();
		}
	}

	@Nested
	@DisplayName("Constructor Tests")
	class ConstructorTests {

		@Test
		@DisplayName("Should create transport with defaults")
		void createWithDefaults() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);

			assertThat(transport.getState()).isEqualTo(SandboxBidirectionalTransport.STATE_DISCONNECTED);
			assertThat(transport.getStateName()).isEqualTo("DISCONNECTED");
		}

		@Test
		@DisplayName("Should create transport with timeout")
		void createWithTimeout() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR, Duration.ofMinutes(5));

			assertThat(transport.getState()).isEqualTo(SandboxBidirectionalTransport.STATE_DISCONNECTED);
		}

		@Test
		@DisplayName("Should create transport with Claude path")
		void createWithClaudePath() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR, Duration.ofMinutes(10),
					"/custom/path/to/claude");

			assertThat(transport.getState()).isEqualTo(SandboxBidirectionalTransport.STATE_DISCONNECTED);
		}

		@Test
		@DisplayName("Should create transport with Sandbox")
		void createWithSandbox() {
			Sandbox mockSandbox = mock(Sandbox.class);
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR, Duration.ofMinutes(10), null, mockSandbox);

			assertThat(transport.getState()).isEqualTo(SandboxBidirectionalTransport.STATE_DISCONNECTED);
		}

		@Test
		@DisplayName("Should reject null working directory")
		void rejectNullWorkingDirectory() {
			assertThatThrownBy(() -> new SandboxBidirectionalTransport(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("workingDirectory must not be null");
		}

		@Test
		@DisplayName("Should reject null timeout")
		void rejectNullTimeout() {
			assertThatThrownBy(() -> new SandboxBidirectionalTransport(TEST_WORKING_DIR, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("defaultTimeout must not be null");
		}

	}

	@Nested
	@DisplayName("Command Building Tests")
	class CommandBuildingTests {

		@Test
		@DisplayName("Should build bidirectional command with required flags")
		void buildBidirectionalCommand() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);
			CLIOptions options = CLIOptions.builder().build();

			List<String> command = transport.buildBidirectionalCommand(options);

			assertThat(command).contains("--input-format", "stream-json");
			assertThat(command).contains("--output-format", "stream-json");
			assertThat(command).contains("--permission-prompt-tool", "stdio");
			assertThat(command).contains("--verbose");
		}

		@Test
		@DisplayName("Should include model in command")
		void buildCommandWithModel() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);
			CLIOptions options = CLIOptions.builder().model("claude-sonnet-4-20250514").build();

			List<String> command = transport.buildBidirectionalCommand(options);

			assertThat(command).contains("--model", "claude-sonnet-4-20250514");
		}

		@Test
		@DisplayName("Should include system prompt in command")
		void buildCommandWithSystemPrompt() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);
			CLIOptions options = CLIOptions.builder().systemPrompt("Be concise").build();

			List<String> command = transport.buildBidirectionalCommand(options);

			assertThat(command).contains("--append-system-prompt", "Be concise");
		}

		@Test
		@DisplayName("Should include allowed tools in command")
		void buildCommandWithAllowedTools() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);
			CLIOptions options = CLIOptions.builder().allowedTools(List.of("Read", "Write")).build();

			List<String> command = transport.buildBidirectionalCommand(options);

			assertThat(command).contains("--allowed-tools", "Read,Write");
		}

		@Test
		@DisplayName("Should include max turns in command")
		void buildCommandWithMaxTurns() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);
			CLIOptions options = CLIOptions.builder().maxTurns(5).build();

			List<String> command = transport.buildBidirectionalCommand(options);

			assertThat(command).contains("--max-turns", "5");
		}

		@Test
		@DisplayName("Should include max budget in command")
		void buildCommandWithMaxBudget() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);
			CLIOptions options = CLIOptions.builder().maxBudgetUsd(1.0).build();

			List<String> command = transport.buildBidirectionalCommand(options);

			assertThat(command).contains("--max-budget-usd", "1.0");
		}

	}

	@Nested
	@DisplayName("Sandbox Integration Tests")
	class SandboxIntegrationTests {

		@Test
		@DisplayName("Should delegate to sandbox.startInteractive when sandbox is configured")
		void delegateToSandbox() throws Exception {
			// Create mock sandbox and process
			Sandbox mockSandbox = mock(Sandbox.class);
			Process mockProcess = createMockProcess();
			when(mockSandbox.startInteractive(any(ExecSpec.class))).thenReturn(mockProcess);

			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR, Duration.ofMinutes(10), "claude",
					mockSandbox);

			CLIOptions options = CLIOptions.builder().build();

			// Start session - should use sandbox
			transport.startSession("Hello", options, msg -> {
			}, req -> null);

			// Verify sandbox was called
			ArgumentCaptor<ExecSpec> specCaptor = ArgumentCaptor.forClass(ExecSpec.class);
			verify(mockSandbox).startInteractive(specCaptor.capture());

			ExecSpec capturedSpec = specCaptor.getValue();
			assertThat(capturedSpec.command()).contains("claude");
			assertThat(capturedSpec.command()).contains("--input-format");
			assertThat(capturedSpec.command()).contains("stream-json");
		}

		@Test
		@DisplayName("Should pass environment variables to sandbox")
		void passEnvironmentToSandbox() throws Exception {
			Sandbox mockSandbox = mock(Sandbox.class);
			Process mockProcess = createMockProcess();
			when(mockSandbox.startInteractive(any(ExecSpec.class))).thenReturn(mockProcess);

			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR, Duration.ofMinutes(10), "claude",
					mockSandbox);

			CLIOptions options = CLIOptions.builder().env("CUSTOM_VAR", "custom_value").build();

			transport.startSession("Hello", options, msg -> {
			}, req -> null);

			ArgumentCaptor<ExecSpec> specCaptor = ArgumentCaptor.forClass(ExecSpec.class);
			verify(mockSandbox).startInteractive(specCaptor.capture());

			ExecSpec capturedSpec = specCaptor.getValue();
			assertThat(capturedSpec.env()).containsKey("CUSTOM_VAR");
			assertThat(capturedSpec.env().get("CUSTOM_VAR")).isEqualTo("custom_value");
			// Should also include SDK identification
			assertThat(capturedSpec.env()).containsKey("CLAUDE_CODE_ENTRYPOINT");
			assertThat(capturedSpec.env().get("CLAUDE_CODE_ENTRYPOINT")).isEqualTo("sdk-java");
		}

		private Process createMockProcess() throws IOException {
			Process mockProcess = mock(Process.class);

			// Mock I/O streams
			OutputStream mockStdin = new ByteArrayOutputStream();
			InputStream mockStdout = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
			InputStream mockStderr = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

			when(mockProcess.getOutputStream()).thenReturn(mockStdin);
			when(mockProcess.getInputStream()).thenReturn(mockStdout);
			when(mockProcess.getErrorStream()).thenReturn(mockStderr);
			when(mockProcess.isAlive()).thenReturn(true);

			return mockProcess;
		}

	}

	@Nested
	@DisplayName("State Machine Tests")
	class StateMachineTests {

		@Test
		@DisplayName("Should start in DISCONNECTED state")
		void initialState() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);

			assertThat(transport.getState()).isEqualTo(SandboxBidirectionalTransport.STATE_DISCONNECTED);
			assertThat(transport.getStateName()).isEqualTo("DISCONNECTED");
		}

		@Test
		@DisplayName("Should transition to CLOSED on close")
		void closeState() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);
			transport.close();

			assertThat(transport.getState()).isEqualTo(SandboxBidirectionalTransport.STATE_CLOSED);
			assertThat(transport.getStateName()).isEqualTo("CLOSED");
		}

		@Test
		@DisplayName("Should not allow reuse after close")
		void noReuseAfterClose() {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR);
			transport.close();

			CLIOptions options = CLIOptions.builder().build();

			assertThatThrownBy(() -> transport.startSession("test", options, msg -> {
			}, req -> null)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("closed and cannot be reused");
		}

	}

	@Nested
	@DisplayName("Live Integration Tests")
	@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
	class LiveIntegrationTests {

		@Test
		@DisplayName("Should connect to Claude CLI without sandbox")
		void connectWithoutSandbox() throws Exception {
			transport = new SandboxBidirectionalTransport(TEST_WORKING_DIR, Duration.ofMinutes(2));

			CLIOptions options = CLIOptions.builder().maxTurns(1).build();

			transport.startSession("Say just 'hello' and nothing else", options, msg -> {
			}, req -> null);

			assertThat(transport.getState()).isEqualTo(SandboxBidirectionalTransport.STATE_CONNECTED);
			assertThat(transport.isRunning()).isTrue();

			// Wait briefly for response
			boolean completed = transport.waitForCompletion(Duration.ofSeconds(30));
			// May or may not complete in 30 seconds, but should not error
		}

	}

}
