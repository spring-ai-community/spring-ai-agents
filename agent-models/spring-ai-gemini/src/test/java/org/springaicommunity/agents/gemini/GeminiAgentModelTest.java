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

import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.geminisdk.types.Cost;
import org.springaicommunity.agents.geminisdk.types.Metadata;
import org.springaicommunity.agents.geminisdk.types.QueryResult;
import org.springaicommunity.agents.geminisdk.types.ResultStatus;
import org.springaicommunity.agents.geminisdk.types.Usage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GeminiAgentModel.
 *
 * @author Mark Pollack
 */
class GeminiAgentModelTest {

	@Mock
	private GeminiClient mockGeminiClient;

	private GeminiAgentModel agentModel;

	private GeminiAgentOptions defaultOptions;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		defaultOptions = GeminiAgentOptions.builder().model("gemini-pro").timeout(Duration.ofMinutes(5)).build();

		agentModel = new GeminiAgentModel(mockGeminiClient, defaultOptions);
	}

	@Test
	void callSuccessfulTask() throws GeminiSDKException {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Fix the failing test", workingDir).build();

		QueryResult mockResult = new QueryResult(List.of(), createMockMetadata(30000), ResultStatus.SUCCESS);

		when(mockGeminiClient.query(anyString(), any(CLIOptions.class))).thenReturn(mockResult);

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
	void callPartialTask() throws GeminiSDKException {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Complex refactoring task", workingDir).build();

		QueryResult mockResult = new QueryResult(List.of(), createMockMetadata(45000), ResultStatus.PARTIAL);

		when(mockGeminiClient.query(anyString(), any(CLIOptions.class))).thenReturn(mockResult);

		// Act
		AgentResponse result = agentModel.call(request);

		// Assert
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("PARTIAL");
		assertThat(result.getMetadata().getDuration()).isNotNull();
	}

	@Test
	void callErrorTask() throws GeminiSDKException {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Invalid task", workingDir).build();

		QueryResult mockResult = new QueryResult(List.of(), createMockMetadata(5000), ResultStatus.ERROR);

		when(mockGeminiClient.query(anyString(), any(CLIOptions.class))).thenReturn(mockResult);

		// Act
		AgentResponse result = agentModel.call(request);

		// Assert
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("ERROR");
	}

	@Test
	void callWithException() throws GeminiSDKException {
		// Arrange
		Path workingDir = Paths.get("/tmp/test");
		AgentTaskRequest request = AgentTaskRequest.builder("Test exception handling", workingDir).build();

		when(mockGeminiClient.query(anyString(), any(CLIOptions.class)))
			.thenThrow(new GeminiSDKException("CLI not available"));

		// Act
		AgentResponse result = agentModel.call(request);

		// Assert
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("ERROR");
		assertThat(result.getResult().getOutput()).contains("CLI not available");
	}

	@Test
	void isAvailableWhenConnected() throws GeminiSDKException {
		// Arrange - connect() should not throw

		// Act
		boolean available = agentModel.isAvailable();

		// Assert
		assertThat(available).isTrue();
	}

	@Test
	void isNotAvailableWhenDisconnected() throws GeminiSDKException {
		// Arrange
		doThrow(new GeminiSDKException("CLI not found")).when(mockGeminiClient).connect();

		// Act
		boolean available = agentModel.isAvailable();

		// Assert
		assertThat(available).isFalse();
	}

	@Test
	void constructorWithDefaultOptions() {
		// Act
		GeminiAgentModel model = new GeminiAgentModel(mockGeminiClient);

		// Assert - should not throw and should be usable
		assertThat(model).isNotNull();
	}

	private Metadata createMockMetadata(long durationMs) {
		Cost mockCost = Cost.of(java.math.BigDecimal.valueOf(0.01), java.math.BigDecimal.valueOf(0.02));

		Usage mockUsage = Usage.of(100, 50);

		return Metadata.builder()
			.model("gemini-pro")
			.timestamp(Instant.now())
			.duration(Duration.ofMillis(durationMs))
			.usage(mockUsage)
			.cost(mockCost)
			.build();
	}

}