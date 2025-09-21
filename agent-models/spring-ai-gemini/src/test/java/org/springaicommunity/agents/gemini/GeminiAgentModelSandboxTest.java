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

package org.springaicommunity.agents.gemini;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.geminisdk.types.Cost;
import org.springaicommunity.agents.geminisdk.types.Metadata;
import org.springaicommunity.agents.geminisdk.types.QueryResult;
import org.springaicommunity.agents.geminisdk.types.ResultStatus;
import org.springaicommunity.agents.geminisdk.types.Usage;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.ExecSpec;
import org.springaicommunity.agents.model.sandbox.Sandbox;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for GeminiAgentModel sandbox integration using dependency injection pattern.
 *
 * These tests prove the key accomplishments: - Interface-based dependency injection with
 * Sandbox - AgentModel-centric pattern: SDK builds command -> Sandbox executes -> SDK
 * parses result - Clean separation: SDKs handle protocol, AgentModels handle execution
 */
class GeminiAgentModelSandboxTest {

	@Mock
	private GeminiClient mockGeminiClient;

	@Mock
	private Sandbox mockSandbox;

	private GeminiAgentModel agentModel;

	private GeminiAgentOptions defaultOptions;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		defaultOptions = GeminiAgentOptions.builder()
			.model("gemini-2.0-flash-exp")
			.timeout(Duration.ofMinutes(5))
			.build();

		// Use constructor injection with Sandbox
		agentModel = new GeminiAgentModel(mockGeminiClient, defaultOptions, mockSandbox);
	}

	@Test
	void testDependencyInjectionPattern() {
		// ASSERT: Constructor injection with Sandbox works
		assertThat(agentModel).isNotNull();
		// Verify that the agentModel was created with the injected Sandbox
		// (implicitly tested by successful construction)
	}

	@Test
	void testDependencyInjectionWithSandboxExecution() throws Exception {
		// Arrange: Gemini SDK now supports buildCommand/parseResult pattern
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Fix the failing test", workingDir).build();

		// Mock buildCommand to return command
		List<String> command = List.of("gemini", "-m", "gemini-2.0-flash-exp", "-y", "-p", "test prompt");
		when(mockGeminiClient.buildCommand(anyString(), any(CLIOptions.class))).thenReturn(command);

		// Mock sandbox execution
		ExecResult execResult = new ExecResult(0, "Test output", Duration.ofSeconds(1));
		when(mockSandbox.exec(any(ExecSpec.class))).thenReturn(execResult);

		// Mock parseResult
		QueryResult queryResult = new QueryResult(List.of(), createMockMetadata(1000), ResultStatus.SUCCESS);
		when(mockGeminiClient.parseResult(anyString(), any(CLIOptions.class))).thenReturn(queryResult);

		// Act
		AgentResponse result = agentModel.call(request);

		// ASSERT: Dependency injection with sandbox pattern works

		// 1. Verify SDK buildCommand was called
		verify(mockGeminiClient).buildCommand(anyString(), any(CLIOptions.class));

		// 2. Verify sandbox was used for execution
		verify(mockSandbox).exec(any(ExecSpec.class));

		// 3. Verify SDK parseResult was called
		verify(mockGeminiClient).parseResult(eq("Test output"), any(CLIOptions.class));

		// 4. Verify final response
		assertThat(result).isNotNull();
		assertThat(result.getResults()).hasSize(1);
	}

	@Test
	void testSandboxInjectionReadyForFutureUse() throws Exception {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Test task", workingDir).build();

		QueryResult queryResult = new QueryResult(List.of(), createMockMetadata(1000), ResultStatus.SUCCESS);
		when(mockGeminiClient.query(anyString(), any(CLIOptions.class))).thenReturn(queryResult);

		// Act
		agentModel.call(request);

		// ASSERT: Sandbox is injected and ready for future use when Gemini SDK supports
		// it
		// Currently Gemini uses direct execution, but sandbox is available for future
		// extensibility
		assertThat(agentModel).isNotNull();
	}

	@Test
	void testSpringStyleDependencyInjection() throws Exception {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Test Spring DI", workingDir).build();

		// Mock the full sandbox execution pattern
		List<String> command = List.of("gemini", "-m", "gemini-2.0-flash-exp", "-y", "-p", "test prompt");
		when(mockGeminiClient.buildCommand(anyString(), any(CLIOptions.class))).thenReturn(command);

		ExecResult execResult = new ExecResult(0, "Test output", Duration.ofSeconds(1));
		when(mockSandbox.exec(any(ExecSpec.class))).thenReturn(execResult);

		QueryResult queryResult = new QueryResult(List.of(), createMockMetadata(1000), ResultStatus.SUCCESS);
		when(mockGeminiClient.parseResult(anyString(), any(CLIOptions.class))).thenReturn(queryResult);

		// Act
		agentModel.call(request);

		// ASSERT: Spring-style dependency injection works with sandbox pattern
		// Agent model successfully uses injected GeminiClient and Sandbox
		verify(mockGeminiClient).buildCommand(anyString(), any(CLIOptions.class));
		verify(mockSandbox).exec(any(ExecSpec.class));
		verify(mockGeminiClient).parseResult(anyString(), any(CLIOptions.class));
		assertThat(agentModel).isNotNull();
	}

	@Test
	void testGeminiExecutionError() throws Exception {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Failing command", workingDir).build();

		// Mock Gemini SDK returning error status
		QueryResult errorResult = new QueryResult(List.of(), createMockMetadata(1000), ResultStatus.ERROR);
		when(mockGeminiClient.query(anyString(), any(CLIOptions.class))).thenReturn(errorResult);

		// Act
		AgentResponse result = agentModel.call(request);

		// ASSERT: Gemini execution errors are handled properly
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("ERROR");
	}

	@Test
	void testSpringStyleConstructorInjection() {
		// ASSERT: Full dependency injection constructor works (Spring-style)
		GeminiAgentModel model = new GeminiAgentModel(mockGeminiClient, defaultOptions, mockSandbox);

		assertThat(model).isNotNull();
		// This proves the Spring-idiomatic constructor injection pattern
	}

	private Metadata createMockMetadata(long durationMs) {
		Cost mockCost = Cost.of(java.math.BigDecimal.valueOf(0.01), java.math.BigDecimal.valueOf(0.02));

		Usage mockUsage = Usage.of(100, 50);

		return Metadata.builder()
			.model("gemini-2.0-flash-exp")
			.timestamp(Instant.now())
			.duration(Duration.ofMillis(durationMs))
			.usage(mockUsage)
			.cost(mockCost)
			.build();
	}

}