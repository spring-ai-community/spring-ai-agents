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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for SweAgentModel.
 *
 * <p>
 * <strong>Note:</strong> Most integration tests are currently disabled due to a bug in
 * mini-SWE-agent CLI v1.4.2 where the --exit-immediately flag doesn't work properly,
 * causing the agent to loop infinitely instead of terminating after task completion. Only
 * the availability test remains enabled.
 * </p>
 *
 * @author Mark Pollack
 */
@DisabledIfSystemProperty(named = "skipIntegrationTests", matches = "true")
class SweAgentModelIT {

	private SweAgentModel agentModel;

	private String executablePath;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		// First try system property, then environment variable, then hardcoded fallback
		executablePath = System.getProperty("swe.cli.path");
		if (executablePath == null) {
			executablePath = System.getenv("SWE_CLI_PATH");
		}
		if (executablePath == null) {
			executablePath = "/home/mark/.local/bin/mini"; // Hardcoded fallback path
			System.out.println("Using hardcoded path: " + executablePath);
		}

		// Set system property for other components
		System.setProperty("swe.cli.path", executablePath);

		SweCliApi sweCliApi = new SweCliApi(executablePath);
		assumeTrue(sweCliApi.isAvailable(), "SWE Agent CLI not available at: " + executablePath);

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini") // Use same model as configured in mini-SWE-agent
			.timeout(Duration.ofMinutes(1)) // Reduce timeout since it's hanging
			.maxIterations(5) // Limit iterations to prevent infinite loops
			.executablePath(executablePath)
			.build();

		agentModel = new SweAgentModel(sweCliApi, options);
	}

	@Test
	@Disabled("mini-SWE-agent CLI has a bug where --exit-immediately flag doesn't work, causing infinite loops")
	void testFileCreationTask() throws IOException {
		// Given
		String fileName = "hello.py";
		String taskGoal = "Create a simple Python file named " + fileName
				+ " that prints 'Hello, Spring AI SWE Agent!' when executed";

		AgentTaskRequest request = new AgentTaskRequest(taskGoal, tempDir, null);

		// When
		AgentResponse result = agentModel.call(request);

		// Then
		System.out.println("Agent Status: " + result.getResult().getMetadata().getFinishReason());
		System.out.println("Agent Output: " + result.getResult().getOutput());

		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("SUCCESS");
		assertThat(result.getResult().getOutput()).isNotEmpty();
		assertThat(result.getMetadata().getDuration()).isNotNull();

		// Verify the file was created
		Path createdFile = tempDir.resolve(fileName);
		assertThat(createdFile).exists();

		String content = Files.readString(createdFile);
		assertThat(content).contains("Hello, Spring AI SWE Agent!");
	}

	@Test
	@Disabled("mini-SWE-agent CLI has a bug where --exit-immediately flag doesn't work, causing infinite loops")
	void testCodeFixingTask() throws IOException {
		// Given
		// Create a Python file with a syntax error
		Path brokenFile = tempDir.resolve("Broken.py");
		Files.writeString(brokenFile, """
				def greet(name):
					print(f"Hello, {name}"  # Missing closing parenthesis

				if __name__ == "__main__":
					greet("World")
				""");

		String taskGoal = "Fix the syntax error in Broken.py";

		AgentTaskRequest request = new AgentTaskRequest(taskGoal, tempDir, null);

		// When
		AgentResponse result = agentModel.call(request);

		// Then
		System.out.println("Agent Status: " + result.getResult().getMetadata().getFinishReason());
		System.out.println("Agent Output: " + result.getResult().getOutput());

		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("SUCCESS");
		assertThat(result.getResult().getOutput()).isNotEmpty();

		// Verify the file was fixed
		String fixedContent = Files.readString(brokenFile);
		assertThat(fixedContent).contains("print(f\"Hello, {name}\")"); // Should have
																		// closing
																		// parenthesis
	}

	@Test
	@Disabled("mini-SWE-agent CLI has a bug where --exit-immediately flag doesn't work, causing infinite loops")
	void testSimpleAnalysisTask() throws IOException {
		// Given
		// Create a sample Python file to analyze
		Path sampleFile = tempDir.resolve("sample.py");
		Files.writeString(sampleFile, """
				def calculate_sum(a, b):
					return a + b

				def calculate_product(a, b):
					return a * b

				print(calculate_sum(5, 3))
				print(calculate_product(4, 6))
				""");

		String taskGoal = "Analyze the Python code in sample.py and add meaningful docstrings to all functions";

		AgentTaskRequest request = new AgentTaskRequest(taskGoal, tempDir, null);

		// When
		AgentResponse result = agentModel.call(request);

		// Then
		System.out.println("Agent Status: " + result.getResult().getMetadata().getFinishReason());
		System.out.println("Agent Output: " + result.getResult().getOutput());

		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("SUCCESS");
		assertThat(result.getResult().getOutput()).isNotEmpty();

		// Verify docstrings were added
		String updatedContent = Files.readString(sampleFile);
		assertThat(updatedContent).contains("\"\"\"");
	}

	@Test
	void testAgentAvailability() {
		// Test that the agent reports as available
		assertThat(agentModel.isAvailable()).isTrue();
	}

	@Test
	@Disabled("mini-SWE-agent CLI has a bug where --exit-immediately flag doesn't work, causing infinite loops")
	void testAgentWithCustomOptions() throws IOException {
		// Given
		SweAgentOptions customOptions = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofMinutes(1))
			.maxIterations(3) // Very low iterations for simple task
			.verbose(false) // Disable verbose to reduce output
			.executablePath(executablePath)
			.build();

		String taskGoal = "Create a simple text file called 'test.txt' with the content 'SWE Agent Test'";

		AgentTaskRequest request = new AgentTaskRequest(taskGoal, tempDir, customOptions);

		// When
		AgentResponse result = agentModel.call(request);

		// Then
		System.out.println("Agent Status: " + result.getResult().getMetadata().getFinishReason());
		System.out.println("Agent Output: " + result.getResult().getOutput());

		assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("SUCCESS");
		assertThat(result.getResult().getOutput()).isNotEmpty();

		// Verify the file was created
		Path createdFile = tempDir.resolve("test.txt");
		assertThat(createdFile).exists();

		String content = Files.readString(createdFile);
		assertThat(content.trim()).isEqualTo("SWE Agent Test");
	}

}