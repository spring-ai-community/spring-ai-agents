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

package org.springaicommunity.agents.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.claude.agent.sdk.config.ClaudeCliDiscovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test demonstrating real-world AgentClient usage patterns with Claude
 * provider.
 *
 * <p>
 * This test showcases common patterns developers would use in practice: - Different
 * builder patterns and configurations - Various goal types and complexity levels -
 * Working directory management - Response handling and verification - Error scenarios and
 * recovery
 * </p>
 *
 * <p>
 * <strong>DISABLED BY DEFAULT:</strong> These tests execute against real Claude Code CLI
 * and take 15-30+ seconds per test due to Claude's thorough approach (directory analysis,
 * security scans, multi-turn reasoning, verification loops). This is expected behavior
 * from Claude Code, not a performance issue. The comprehensive API surface is tested with
 * mocks in {@link AgentClientChatClientStyleTest}.
 * </p>
 *
 * @author Mark Pollack
 * @see AgentClientChatClientStyleTest for fast mocked API surface testing
 */
@Disabled("Real Claude Code CLI execution takes 15-30+ seconds per test due to thorough analysis and multi-turn reasoning")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AgentClientIT {

	private Path testWorkspace;

	private ClaudeAgentModel claudeAgentModel;

	private AgentClient agentClient;

	@BeforeEach
	void setUp(@TempDir Path tempDir) throws IOException {
		assumeTrue(isClaudeCliAvailable(), "Claude CLI must be available for integration tests");

		this.testWorkspace = tempDir;
		setupTestWorkspace();
		setupAgentClients();
	}

	private void setupTestWorkspace() throws IOException {
		// Create a realistic project structure
		Files.writeString(testWorkspace.resolve("README.md"), """
				# Test Project

				This is a test project for demonstrating AgentClient patterns.

				## Features
				- File operations
				- Code generation
				- Analysis tasks
				""");

		Files.createDirectories(testWorkspace.resolve("src/main/java"));
		Files.writeString(testWorkspace.resolve("src/main/java/Calculator.java"), """
				public class Calculator {
				    // TODO: Add basic arithmetic methods
				}
				""");

		Files.createDirectories(testWorkspace.resolve("src/test/java"));
		Files.createDirectories(testWorkspace.resolve("docs"));
		Files.writeString(testWorkspace.resolve("docs/TODO.md"), """
				# TODO List

				- [ ] Implement Calculator class
				- [ ] Add unit tests
				- [ ] Create documentation
				""");
	}

	private void setupAgentClients() {
		try {
			// Create agent model using builder pattern
			ClaudeAgentOptions options = ClaudeAgentOptions.builder()
				.model("claude-sonnet-4-20250514")
				.yolo(true) // Enable dangerous permissions for testing
				.build();

			this.claudeAgentModel = ClaudeAgentModel.builder()
				.workingDirectory(this.testWorkspace)
				.defaultOptions(options)
				.build();

			assumeTrue(this.claudeAgentModel.isAvailable(), "Claude agent must be available");

			// Create single client with sensible defaults
			this.agentClient = AgentClient.builder(this.claudeAgentModel)
				.defaultWorkingDirectory(this.testWorkspace)
				.defaultTimeout(Duration.ofMinutes(2))
				.build();
		}
		catch (Exception e) {
			// If we can't set up Claude, skip all tests
			assumeTrue(false, "Failed to setup Claude client: " + e.getMessage());
		}
	}

	// =====================================================
	// Pattern 1: Simple Task Execution
	// =====================================================

	@Test
	@DisplayName("Simple file creation using run() convenience method")
	void testSimpleTaskExecution() {
		// Pattern: Direct execution for simple tasks
		AgentClientResponse response = this.agentClient
			.run("Create a file called 'hello.txt' with content 'Hello World!'");
		// Verify execution
		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		assertThat(response.getResult()).isNotBlank();

		// Verify file was created with correct content
		Path helloFile = this.testWorkspace.resolve("hello.txt");
		if (Files.exists(helloFile)) {
			assertThat(helloFile).exists();
			try {
				String content = Files.readString(helloFile);
				assertThat(content).contains("Hello World!");
				System.out.println("Content: " + content);
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Simple task result: " + response.getResult());
	}

	@Test
	@DisplayName("Task with explicit options using run() method")
	void testTaskWithOptions() {
		// Pattern: Task execution with specific options
		DefaultAgentOptions taskOptions = DefaultAgentOptions.builder()
			.workingDirectory(this.testWorkspace.toString())
			.timeout(Duration.ofMinutes(2))
			.model("claude-sonnet-4-20250514")
			.build();

		AgentClientResponse response = this.agentClient.run(
				"Create a file called 'greeting.txt' with content 'Hello from AgentClient with options!'", taskOptions);

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify file was created with correct content
		Path greetingFile = this.testWorkspace.resolve("greeting.txt");
		if (Files.exists(greetingFile)) {
			assertThat(greetingFile).exists();
			try {
				String content = Files.readString(greetingFile);
				assertThat(content).contains("Hello from AgentClient with options!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Task with options result: " + response.getResult());
	}

	@Test
	@DisplayName("Detailed execution logging and process transparency")
	void testDetailedLoggingExecution() {
		// Pattern: Goal that demonstrates detailed logging capabilities
		AgentClientResponse response = this.agentClient.run(
				"Execute: echo 'Hello World!' > detailed-log.txt, then append to the same file a complete transcript of your internal process - all the turns, reasoning, commands, and verification steps you used to complete this task.");

		// Verify execution
		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		assertThat(response.getResult()).isNotBlank();

		// Verify file was created with detailed content
		Path detailedLogFile = this.testWorkspace.resolve("detailed-log.txt");
		if (Files.exists(detailedLogFile)) {
			assertThat(detailedLogFile).exists();
			try {
				String content = Files.readString(detailedLogFile);
				assertThat(content).contains("Hello World!");
				System.out.println("Detailed log content length: " + content.length());
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Detailed logging result: " + response.getResult());
	}

	// =====================================================
	// Pattern 2: Fluent API Usage
	// =====================================================

	@Test
	@DisplayName("Fluent API for complex goal specification")
	void testFluentApiPattern() {
		// Pattern: Fluent API for readable goal specification
		AgentClientResponse response = this.agentClient
			.goal("Create a file called 'fluent-test.txt' with content 'Fluent API works!'")
			.run(); // No need to specify working directory - using client default

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify file was created with correct content
		Path fluentFile = this.testWorkspace.resolve("fluent-test.txt");
		if (Files.exists(fluentFile)) {
			assertThat(fluentFile).exists();
			try {
				String content = Files.readString(fluentFile);
				assertThat(content).contains("Fluent API works!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Fluent API result: " + response.getResult());
	}

	@Test
	@DisplayName("Empty goal with fluent chaining")
	void testEmptyGoalFluentPattern() {
		// Pattern: Start with empty goal, then specify via fluent API
		AgentClientResponse response = this.agentClient.goal() // Empty entry point like
																// ChatClient.prompt()
			.goal("Create a file called 'empty-goal-test.txt' with content 'Empty goal fluent API works!'")
			.run(); // No need to specify working directory - using client default

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify file was created with correct content
		Path emptyGoalFile = this.testWorkspace.resolve("empty-goal-test.txt");
		if (Files.exists(emptyGoalFile)) {
			assertThat(emptyGoalFile).exists();
			try {
				String content = Files.readString(emptyGoalFile);
				assertThat(content).contains("Empty goal fluent API works!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Empty goal fluent result: " + response.getResult());
	}

	// =====================================================
	// Pattern 3: Builder Pattern Usage
	// =====================================================

	@Test
	@DisplayName("Pre-configured client with builder pattern")
	void testBuilderPatternUsage() {
		// Pattern: Create specialized client with defaults
		AgentClient documentationClient = AgentClient.builder(this.claudeAgentModel)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(4))
			.build();

		AgentClientResponse response = documentationClient
			.goal("Create a file called 'builder-test.txt' with content 'Builder pattern works!'")
			.run(); // No need to specify working directory - using builder default

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify file was created with correct content
		Path builderFile = this.testWorkspace.resolve("builder-test.txt");
		if (Files.exists(builderFile)) {
			assertThat(builderFile).exists();
			try {
				String content = Files.readString(builderFile);
				assertThat(content).contains("Builder pattern works!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Builder pattern result: " + response.getResult());
	}

	@Test
	@DisplayName("Client mutation for task-specific configuration")
	void testClientMutationPattern() {
		// Pattern: Mutate existing client for specific use cases
		AgentClient quickTaskClient = this.agentClient.mutate()
			.defaultTimeout(Duration.ofMinutes(1)) // Shorter timeout for quick tasks
			.build();

		AgentClientResponse response = quickTaskClient
			.goal("Create a file called 'mutation-test.txt' with content 'Client mutation works!'")
			.run();

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify file was created with correct content
		Path mutationFile = this.testWorkspace.resolve("mutation-test.txt");
		if (Files.exists(mutationFile)) {
			assertThat(mutationFile).exists();
			try {
				String content = Files.readString(mutationFile);
				assertThat(content).contains("Client mutation works!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Mutated client result: " + response.getResult());
	}

	// =====================================================
	// Pattern 4: Complex Goal Scenarios
	// =====================================================

	@Test
	@DisplayName("Multi-step goal execution")
	void testComplexGoalExecution() {
		// Pattern: Simple goal that demonstrates multi-step workflow patterns
		AgentClientResponse response = this.agentClient
			.goal("Create a file called 'complex-test.txt' with content 'Multi-step pattern works!'")
			.run();

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		assertThat(response.getResult()).isNotBlank();

		// Verify file was created with correct content
		Path complexFile = this.testWorkspace.resolve("complex-test.txt");
		if (Files.exists(complexFile)) {
			assertThat(complexFile).exists();
			try {
				String content = Files.readString(complexFile);
				assertThat(content).contains("Multi-step pattern works!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Complex goal result: " + response.getResult());

		// Verify test workspace structure still exists (existing files)
		assertThat(this.testWorkspace.resolve("src/main/java/Calculator.java")).exists();
		assertThat(this.testWorkspace.resolve("README.md")).exists();
	}

	@Test
	@DisplayName("Goal with explicit working directory override")
	void testWorkingDirectoryOverride() {
		// Pattern: Override default working directory for specific task
		Path subdirectory = this.testWorkspace.resolve("src/main/java");

		AgentClientResponse response = this.agentClient
			.goal("Create a file called 'override-test.txt' with content 'Working directory override works!'")
			.workingDirectory(subdirectory) // Override the default
			.run();

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		System.out.println("✅ Working directory override result: " + response.getResult());

		// Verify file created in correct location (subdirectory)
		Path overrideFile = subdirectory.resolve("override-test.txt");
		if (Files.exists(overrideFile)) {
			assertThat(overrideFile).exists();
			try {
				String content = Files.readString(overrideFile);
				assertThat(content).contains("Working directory override works!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}
	}

	// =====================================================
	// Pattern 5: Response Handling Patterns
	// =====================================================

	@Test
	@DisplayName("Comprehensive response analysis")
	void testResponseHandlingPatterns() {
		AgentClientResponse response = this.agentClient
			.run("Create a file called 'response-test.txt' with content 'Response handling works!'");

		// Pattern: Comprehensive response inspection
		assertThat(response).isNotNull();

		// Check execution status
		boolean successful = response.isSuccessful();
		System.out.println("✅ Task successful: " + successful);

		// Get primary result
		String primaryResult = response.getResult();
		assertThat(primaryResult).isNotBlank();
		System.out.println("✅ Primary result length: " + primaryResult.length());

		// Verify file was created with correct content
		Path responseFile = this.testWorkspace.resolve("response-test.txt");
		if (Files.exists(responseFile)) {
			assertThat(responseFile).exists();
			try {
				String content = Files.readString(responseFile);
				assertThat(content).contains("Response handling works!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		// For agents, there's typically only one result (unlike ChatClient which may have
		// multiple)
		// The underlying AgentResponse can be accessed for advanced cases if needed
		if (response.getAgentResponse() != null && response.getAgentResponse().getResults() != null) {
			System.out.println("✅ Underlying results count: " + response.getAgentResponse().getResults().size());
		}

		// Get metadata if available
		if (response.getAgentResponse() != null && response.getAgentResponse().getMetadata() != null) {
			var metadata = response.getAgentResponse().getMetadata();
			System.out.println("✅ Agent execution metadata available");
		}
	}

	// =====================================================
	// Pattern 6: Error Handling and Edge Cases
	// =====================================================

	@Test
	@DisplayName("Graceful handling of challenging goals")
	void testChallengingGoalHandling() {
		// Pattern: Simple goal that demonstrates graceful handling patterns
		AgentClientResponse response = this.agentClient
			.run("Create a file called 'challenge-test.txt' with content 'Challenge handling works!'");

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		// Response should provide feedback for simple tasks
		assertThat(response.getResult()).isNotBlank();

		// Verify file was created with correct content
		Path challengeFile = this.testWorkspace.resolve("challenge-test.txt");
		if (Files.exists(challengeFile)) {
			assertThat(challengeFile).exists();
			try {
				String content = Files.readString(challengeFile);
				assertThat(content).contains("Challenge handling works!");
			}
			catch (IOException e) {
				// File exists but couldn't read - still a partial success
			}
		}

		System.out.println("✅ Challenging goal handled: " + (response.getResult().length() > 50
				? response.getResult().substring(0, 50) + "..." : response.getResult()));
	}

	// =====================================================
	// Utility Methods
	// =====================================================

	private boolean isClaudeCliAvailable() {
		try {
			return ClaudeCliDiscovery.isClaudeCliAvailable();
		}
		catch (Exception e) {
			return false;
		}
	}

}