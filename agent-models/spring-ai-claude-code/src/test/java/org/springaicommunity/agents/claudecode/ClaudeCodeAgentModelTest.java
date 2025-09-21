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

import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;
import org.springaicommunity.agents.claudecode.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claudecode.sdk.types.Cost;
import org.springaicommunity.agents.claudecode.sdk.types.Metadata;
import org.springaicommunity.agents.claudecode.sdk.types.QueryResult;
import org.springaicommunity.agents.claudecode.sdk.types.ResultStatus;
import org.springaicommunity.agents.claudecode.sdk.types.Usage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springaicommunity.agents.model.sandbox.TimeoutException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClaudeCodeAgentModel.
 *
 * @author Mark Pollack
 */
class ClaudeCodeAgentModelTest {

	@Mock
	private ClaudeCodeClient mockClaudeCodeClient;

	@Mock
	private Sandbox mockSandbox;

	private ClaudeCodeAgentModel agentModel;

	private ClaudeCodeAgentOptions defaultOptions;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		defaultOptions = ClaudeCodeAgentOptions.builder()
			.model("claude-sonnet-4-20250514")
			.timeout(Duration.ofMinutes(5))
			.build();

		agentModel = new ClaudeCodeAgentModel(mockClaudeCodeClient, defaultOptions, mockSandbox);
	}

	@Test
	void callSuccessfulTask() {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Fix the failing test", workingDir).build();

		QueryResult mockResult = new QueryResult(List.of(), createMockMetadata(30000), ResultStatus.SUCCESS);

		// Mock the buildCommand method for sandbox execution
		when(mockClaudeCodeClient.buildCommand(anyString(), any(CLIOptions.class)))
			.thenReturn(List.of("claude", "--print", "Fix the failing test"));

		// Mock sandbox execution
		try {
			ExecResult mockExecResult = new ExecResult(0, "Mock Claude output", Duration.ofSeconds(5));
			when(mockSandbox.exec(any())).thenReturn(mockExecResult);
		}
		catch (Exception e) {
			throw new RuntimeException("Test setup failed", e);
		}

		// Mock parseResult method
		when(mockClaudeCodeClient.parseResult(anyString(), any(CLIOptions.class))).thenReturn(mockResult);

		when(mockClaudeCodeClient.query(anyString(), any(CLIOptions.class))).thenReturn(mockResult);

		// Act
		AgentResponse result = agentModel.call(request);

		// Assert
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("SUCCESS");
		assertThat(result.getMetadata().getDuration()).isNotNull();
		assertThat(result.getResult().getOutput()).isNotNull();
		assertThat(result.getMetadata().getProviderFields()).isNotNull();
	}

	@Test
	void callPartialTask() {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Complex refactoring task", workingDir).build();

		QueryResult mockResult = new QueryResult(List.of(), createMockMetadata(45000), ResultStatus.PARTIAL);

		// Mock the buildCommand method for sandbox execution
		when(mockClaudeCodeClient.buildCommand(anyString(), any(CLIOptions.class)))
			.thenReturn(List.of("claude", "--print", "Complex refactoring task"));

		// Mock sandbox execution
		try {
			ExecResult mockExecResult = new ExecResult(0, "Mock Claude output", Duration.ofSeconds(45));
			when(mockSandbox.exec(any())).thenReturn(mockExecResult);
		}
		catch (Exception e) {
			throw new RuntimeException("Test setup failed", e);
		}

		// Mock parseResult method
		when(mockClaudeCodeClient.parseResult(anyString(), any(CLIOptions.class))).thenReturn(mockResult);

		when(mockClaudeCodeClient.query(anyString(), any(CLIOptions.class))).thenReturn(mockResult);

		// Act
		AgentResponse result = agentModel.call(request);

		// Assert
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("PARTIAL");
		assertThat(result.getMetadata().getDuration()).isNotNull();
	}

	@Test
	void callErrorTask() throws ClaudeSDKException {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Invalid task", workingDir).build();

		QueryResult mockResult = new QueryResult(List.of(), createMockMetadata(5000), ResultStatus.ERROR);

		try {
			when(mockClaudeCodeClient.query(anyString(), any(CLIOptions.class))).thenReturn(mockResult);
		}
		catch (ClaudeSDKException e) {
			throw new RuntimeException("Mock setup failed", e);
		}

		// Act
		AgentResponse result = agentModel.call(request);

		// Assert
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("ERROR");
	}

	@Test
	void callWithException() {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Test exception handling", workingDir).build();

		// Mock the buildCommand method - it should work
		when(mockClaudeCodeClient.buildCommand(anyString(), any(CLIOptions.class)))
			.thenReturn(List.of("claude", "--print", "Test exception handling"));

		// Mock sandbox execution to fail
		try {
			when(mockSandbox.exec(any())).thenThrow(new RuntimeException("CLI not available"));
		}
		catch (Exception e) {
			throw new RuntimeException("Test setup failed", e);
		}

		when(mockClaudeCodeClient.query(anyString(), any(CLIOptions.class)))
			.thenThrow(new ClaudeSDKException("CLI not available"));

		// Act
		AgentResponse result = agentModel.call(request);

		// Assert
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("ERROR");
		assertThat(result.getResult().getOutput()).contains("CLI not available");
	}

	@Test
	void isAvailableWhenConnected() throws ClaudeSDKException {
		// Arrange - connect() should not throw

		// Act
		boolean available = agentModel.isAvailable();

		// Assert
		assertThat(available).isTrue();
	}

	@Test
	void isNotAvailableWhenDisconnected() throws ClaudeSDKException {
		// Arrange
		doThrow(new ClaudeSDKException("CLI not found")).when(mockClaudeCodeClient).connect();

		// Act
		boolean available = agentModel.isAvailable();

		// Assert
		assertThat(available).isFalse();
	}

	@Test
	void constructorWithDefaultOptions() {
		// Act
		ClaudeCodeAgentModel model = new ClaudeCodeAgentModel(mockClaudeCodeClient, new ClaudeCodeAgentOptions(),
				mockSandbox);

		// Assert - should not throw and should be usable
		assertThat(model).isNotNull();
	}

	private Metadata createMockMetadata(long durationMs) {
		Cost mockCost = Cost.builder()
			.model("claude-sonnet-4-20250514")
			.inputTokens(100)
			.outputTokens(50)
			.inputTokenCost(0.01)
			.outputTokenCost(0.02)
			.build();

		Usage mockUsage = Usage.builder().inputTokens(100).outputTokens(50).thinkingTokens(25).build();

		return Metadata.builder()
			.model("claude-sonnet-4-20250514")
			.cost(mockCost)
			.usage(mockUsage)
			.durationMs(durationMs)
			.apiDurationMs(durationMs - 1000)
			.sessionId("test-session")
			.numTurns(1)
			.build();
	}

}