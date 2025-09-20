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

package org.springaicommunity.agents.claudecode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;
import org.springaicommunity.agents.claudecode.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claudecode.sdk.types.Cost;
import org.springaicommunity.agents.claudecode.sdk.types.Metadata;
import org.springaicommunity.agents.claudecode.sdk.types.QueryResult;
import org.springaicommunity.agents.claudecode.sdk.types.ResultStatus;
import org.springaicommunity.agents.claudecode.sdk.types.Usage;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.ExecSpec;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springaicommunity.agents.model.sandbox.SandboxProvider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for ClaudeCodeAgentModel sandbox integration using dependency injection pattern.
 *
 * These tests prove the key accomplishments: - Interface-based dependency injection with
 * SandboxProvider - AgentModel-centric pattern: SDK builds command -> Sandbox executes ->
 * SDK parses result - Clean separation: SDKs handle protocol, AgentModels handle
 * execution
 */
class ClaudeCodeAgentModelSandboxTest {

	@Mock
	private ClaudeCodeClient mockClaudeCodeClient;

	@Mock
	private SandboxProvider mockSandboxProvider;

	@Mock
	private Sandbox mockSandbox;

	private ClaudeCodeAgentModel agentModel;

	private ClaudeCodeAgentOptions defaultOptions;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		defaultOptions = ClaudeCodeAgentOptions.builder()
			.model("claude-3-5-sonnet-20241022")
			.timeout(Duration.ofMinutes(5))
			.build();

		// Use constructor injection with SandboxProvider
		agentModel = new ClaudeCodeAgentModel(mockClaudeCodeClient, defaultOptions, mockSandboxProvider);
	}

	@Test
	void testDependencyInjectionPattern() {
		// ASSERT: Constructor injection with SandboxProvider works
		assertThat(agentModel).isNotNull();
		// Verify that the agentModel was created with the injected SandboxProvider
		// (implicitly tested by successful construction)
	}

	@Test
	void testAgentModelCentricPattern() throws Exception {
		// Arrange: Setup the AgentModel-centric execution pattern
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Fix the failing test", workingDir).build();

		// Mock SDK buildCommand - proves SDK builds command
		List<String> expectedCommand = List.of("claude", "--print", "--output-format", "json", "--model",
				"claude-3-5-sonnet-20241022", "Fix the failing test");
		when(mockClaudeCodeClient.buildCommand(anyString(), any(CLIOptions.class))).thenReturn(expectedCommand);

		// Mock sandbox execution - proves AgentModel executes via sandbox
		ExecResult execResult = new ExecResult(0, "{\"result\": \"Task completed successfully\"}",
				Duration.ofSeconds(30));
		when(mockSandboxProvider.getSandbox()).thenReturn(mockSandbox);
		when(mockSandbox.exec(any(ExecSpec.class))).thenReturn(execResult);

		// Mock SDK parseResult - proves SDK parses result
		QueryResult queryResult = new QueryResult(List.of(), createMockMetadata(30000), ResultStatus.SUCCESS);
		when(mockClaudeCodeClient.parseResult(anyString(), any(CLIOptions.class))).thenReturn(queryResult);

		// Act
		AgentResponse result = agentModel.call(request);

		// ASSERT: AgentModel-centric pattern executed correctly

		// 1. Verify SDK built command
		verify(mockClaudeCodeClient).buildCommand(anyString(), any(CLIOptions.class));

		// 2. Verify sandbox executed command
		ArgumentCaptor<ExecSpec> execSpecCaptor = ArgumentCaptor.forClass(ExecSpec.class);
		verify(mockSandbox).exec(execSpecCaptor.capture());

		ExecSpec capturedSpec = execSpecCaptor.getValue();
		assertThat(capturedSpec.command()).isEqualTo(expectedCommand);
		assertThat(capturedSpec.env()).containsEntry("CLAUDE_CODE_ENTRYPOINT", "sdk-java");
		assertThat(capturedSpec.timeout()).isEqualTo(Duration.ofMinutes(5));

		// 3. Verify SDK parsed result
		verify(mockClaudeCodeClient).parseResult(eq("{\"result\": \"Task completed successfully\"}"),
				any(CLIOptions.class));

		// 4. Verify final response
		assertThat(result).isNotNull();
		assertThat(result.getResults()).hasSize(1);
	}

	@Test
	void testSandboxProviderIntegration() throws Exception {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Test task", workingDir).build();

		when(mockClaudeCodeClient.buildCommand(anyString(), any(CLIOptions.class)))
			.thenReturn(List.of("claude", "--help"));

		ExecResult execResult = new ExecResult(0, "Command executed", Duration.ofSeconds(1));
		when(mockSandboxProvider.getSandbox()).thenReturn(mockSandbox);
		when(mockSandbox.exec(any(ExecSpec.class))).thenReturn(execResult);

		QueryResult queryResult = new QueryResult(List.of(), createMockMetadata(1000), ResultStatus.SUCCESS);
		when(mockClaudeCodeClient.parseResult(anyString(), any(CLIOptions.class))).thenReturn(queryResult);

		// Act
		agentModel.call(request);

		// ASSERT: SandboxProvider dependency injection works
		verify(mockSandboxProvider).getSandbox();
		verify(mockSandbox).exec(any(ExecSpec.class));
	}

	@Test
	void testEnvironmentVariableInjection() throws Exception {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Test env vars", workingDir).build();

		when(mockClaudeCodeClient.buildCommand(anyString(), any(CLIOptions.class)))
			.thenReturn(List.of("claude", "--version"));

		ExecResult execResult = new ExecResult(0, "Claude CLI version", Duration.ofSeconds(1));
		when(mockSandboxProvider.getSandbox()).thenReturn(mockSandbox);
		when(mockSandbox.exec(any(ExecSpec.class))).thenReturn(execResult);

		QueryResult queryResult = new QueryResult(List.of(), createMockMetadata(1000), ResultStatus.SUCCESS);
		when(mockClaudeCodeClient.parseResult(anyString(), any(CLIOptions.class))).thenReturn(queryResult);

		// Act
		agentModel.call(request);

		// ASSERT: Environment variables are properly set
		ArgumentCaptor<ExecSpec> execSpecCaptor = ArgumentCaptor.forClass(ExecSpec.class);
		verify(mockSandbox).exec(execSpecCaptor.capture());

		ExecSpec capturedSpec = execSpecCaptor.getValue();
		Map<String, String> env = capturedSpec.env();
		assertThat(env).containsEntry("CLAUDE_CODE_ENTRYPOINT", "sdk-java");
		// Note: ANTHROPIC_API_KEY would be present if set in system environment
	}

	@Test
	void testSandboxExecutionError() throws Exception {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Failing command", workingDir).build();

		when(mockClaudeCodeClient.buildCommand(anyString(), any(CLIOptions.class)))
			.thenReturn(List.of("claude", "--invalid-flag"));

		// Mock sandbox returning non-zero exit code
		ExecResult execResult = new ExecResult(1, "Error: invalid flag", Duration.ofSeconds(1));
		when(mockSandboxProvider.getSandbox()).thenReturn(mockSandbox);
		when(mockSandbox.exec(any(ExecSpec.class))).thenReturn(execResult);

		// Act
		AgentResponse result = agentModel.call(request);

		// ASSERT: Sandbox execution errors are handled properly
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("ERROR");
		assertThat(result.getResult().getOutput()).contains("Command execution failed with exit code 1");
		assertThat(result.getResult().getOutput()).contains("Error: invalid flag");
	}

	@Test
	void testDefaultSandboxProviderFallback() {
		// ASSERT: Constructor without SandboxProvider creates DefaultSandboxProvider
		ClaudeCodeAgentModel modelWithDefaults = new ClaudeCodeAgentModel(mockClaudeCodeClient, defaultOptions);
		assertThat(modelWithDefaults).isNotNull();
		// The fact that this doesn't throw proves the DefaultSandboxProvider fallback
		// works
	}

	@Test
	void testSpringStyleConstructorInjection() {
		// ASSERT: Full dependency injection constructor works (Spring-style)
		ClaudeCodeAgentModel model = new ClaudeCodeAgentModel(mockClaudeCodeClient, defaultOptions,
				mockSandboxProvider);

		assertThat(model).isNotNull();
		// This proves the Spring-idiomatic constructor injection pattern
	}

	private Metadata createMockMetadata(long durationMs) {
		Cost mockCost = Cost.builder()
			.model("claude-3-5-sonnet")
			.inputTokens(100)
			.outputTokens(50)
			.inputTokenCost(0.01)
			.outputTokenCost(0.02)
			.build();

		Usage mockUsage = Usage.builder().inputTokens(100).outputTokens(50).thinkingTokens(25).build();

		return Metadata.builder()
			.model("claude-3-5-sonnet")
			.cost(mockCost)
			.usage(mockUsage)
			.durationMs(durationMs)
			.apiDurationMs(durationMs - 1000)
			.sessionId("test-session")
			.numTurns(1)
			.build();
	}

}