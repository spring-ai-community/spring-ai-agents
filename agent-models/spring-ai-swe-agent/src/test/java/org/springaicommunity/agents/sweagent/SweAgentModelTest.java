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

package org.springaicommunity.agents.sweagent;

import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi;
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi.SweResult;
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi.SweResultStatus;
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi.SweCliException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

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
 * Unit tests for SweAgentModel.
 *
 * @author Mark Pollack
 */
class SweAgentModelTest {

	@Mock
	private SweCliApi mockSweCliApi;

	private SweAgentModel agentModel;

	private Path testWorkingDirectory;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		agentModel = new SweAgentModel(mockSweCliApi);
		testWorkingDirectory = Paths.get("/tmp/test-workspace");
	}

	@Test
	void testSuccessfulExecution() throws Exception {
		// Given
		String taskGoal = "Fix the bug in the authentication module";
		AgentTaskRequest request = new AgentTaskRequest(taskGoal, testWorkingDirectory, null);

		SweResult mockResult = new SweResult(SweResultStatus.SUCCESS,
				"Task completed successfully. Fixed authentication bug in auth.py", "", null);
		when(mockSweCliApi.execute(anyString(), any(Path.class), any())).thenReturn(mockResult);

		// When
		AgentResponse result = agentModel.call(request);

		// Then
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("SUCCESS");
		assertThat(result.getResult().getOutput()).contains("Task completed successfully");
		assertThat(result.getMetadata().getDuration()).isNotNull();
	}

	@Test
	void testFailedExecution() throws Exception {
		// Given
		String taskGoal = "Implement a new feature";
		AgentTaskRequest request = new AgentTaskRequest(taskGoal, testWorkingDirectory, null);

		SweResult mockResult = new SweResult(SweResultStatus.ERROR, "",
				"Failed to implement feature: missing dependencies", null);
		when(mockSweCliApi.execute(anyString(), any(Path.class), any())).thenReturn(mockResult);

		// When
		AgentResponse result = agentModel.call(request);

		// Then
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("ERROR");
		assertThat(result.getResult().getOutput()).contains("missing dependencies");
	}

	@Test
	void testExecutionWithOptions() throws Exception {
		// Given
		String taskGoal = "Refactor the codebase";
		SweAgentOptions options = SweAgentOptions.builder()
			.model("claude-3-5-sonnet")
			.timeout(Duration.ofMinutes(10))
			.maxIterations(15)
			.verbose(true)
			.build();
		AgentTaskRequest request = new AgentTaskRequest(taskGoal, testWorkingDirectory, options);

		SweResult mockResult = new SweResult(SweResultStatus.SUCCESS, "Refactoring completed", "", null);
		when(mockSweCliApi.execute(anyString(), any(Path.class), any())).thenReturn(mockResult);

		// When
		AgentResponse result = agentModel.call(request);

		// Then
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("SUCCESS");
	}

	@Test
	void testCliException() throws Exception {
		// Given
		String taskGoal = "Test CLI exception handling";
		AgentTaskRequest request = new AgentTaskRequest(taskGoal, testWorkingDirectory, null);

		when(mockSweCliApi.execute(anyString(), any(Path.class), any()))
			.thenThrow(new SweCliException("CLI execution failed"));

		// When
		AgentResponse result = agentModel.call(request);

		// Then
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("ERROR");
		assertThat(result.getResult().getOutput()).contains("CLI execution failed");
	}

	@Test
	void testIsAvailable() {
		// Given
		when(mockSweCliApi.isAvailable()).thenReturn(true);

		// When
		boolean available = agentModel.isAvailable();

		// Then
		assertThat(available).isTrue();
	}

	@Test
	void testIsNotAvailable() {
		// Given
		when(mockSweCliApi.isAvailable()).thenReturn(false);

		// When
		boolean available = agentModel.isAvailable();

		// Then
		assertThat(available).isFalse();
	}

	@Test
	void testBuilderOptions() {
		// Test the builder pattern works correctly
		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4")
			.timeout(Duration.ofMinutes(15))
			.maxIterations(25)
			.verbose(false)
			.build();

		assertThat(options.getModel()).isEqualTo("gpt-4");
		assertThat(options.getTimeout()).isEqualTo(Duration.ofMinutes(15));
		assertThat(options.getMaxIterations()).isEqualTo(25);
		assertThat(options.isVerbose()).isFalse();
	}

}