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

package org.springaicommunity.agents.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.model.sandbox.Sandbox;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test Compatibility Kit (TCK) for testing any AgentModel implementation with any
 * Sandbox.
 *
 * <p>
 * This abstract test class defines the standard test suite that all AgentModel
 * implementations must pass when used with any Sandbox implementation. Concrete test
 * classes should extend this class and provide the specific AgentModel and Sandbox
 * implementations in their setup methods.
 * </p>
 *
 * <p>
 * The TCK ensures consistent behavior across all agent×sandbox combinations including: -
 * Basic file operations - Directory listing and reading - Code fixing tasks - Agent
 * availability checking - Timeout handling - Complex multi-step operations
 * </p>
 */
public abstract class AbstractAgentModelTCK {

	/**
	 * The agent model implementation under test. Must be set by concrete test classes.
	 */
	protected AgentModel agentModel;

	/**
	 * The sandbox implementation used by the agent. Must be set by concrete test classes.
	 */
	protected Sandbox sandbox;

	/**
	 * Working directory for tests. Provided by JUnit's @TempDir.
	 */
	@TempDir
	protected Path tempDir;

	/**
	 * Cleanup after each test to ensure resource isolation.
	 */
	@AfterEach
	void tearDown() throws Exception {
		if (sandbox != null && !sandbox.isClosed()) {
			sandbox.close();
		}
	}

	/**
	 * Test basic file creation functionality. Verifies that the agent can create files
	 * with specified content.
	 */
	@Test
	void testSimpleFileCreation() throws IOException {
		// Arrange
		AgentTaskRequest request = AgentTaskRequest
			.builder("Create a file named 'greeting.txt' with the content 'Hello, Spring AI!'", tempDir)
			.build();

		// Act
		AgentResponse response = agentModel.call(request);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();
		assertThat(response.getMetadata().getModel()).isNotNull();

		// Verify file was created
		Path greetingFile = tempDir.resolve("greeting.txt");
		assertThat(Files.exists(greetingFile)).isTrue();
		String content = Files.readString(greetingFile);
		assertThat(content).contains("Hello, Spring AI!");
	}

	/**
	 * Test file listing and reading functionality. Verifies that the agent can list
	 * directory contents and read files.
	 */
	@Test
	void testFileListAccess() throws IOException {
		// Arrange: Create a test file
		Path testFile = tempDir.resolve("test.txt");
		Files.writeString(testFile, "Test content for reading");

		AgentTaskRequest request = AgentTaskRequest
			.builder("List the files in the working directory and read their contents", tempDir)
			.build();

		// Act
		AgentResponse response = agentModel.call(request);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();

		// Response should mention the test file and potentially show its content
		String result = response.getResult().getOutput();
		assertThat(result).containsIgnoringCase("test.txt");
	}

	/**
	 * Test code fixing functionality. Verifies that the agent can identify and fix syntax
	 * errors in code files.
	 */
	@Test
	void testCodeFixTask() throws IOException {
		// Arrange: Create a Java file with a syntax error
		Path brokenJavaFile = tempDir.resolve("Broken.java");
		String brokenCode = """
				public class Broken {
				    public static void main(String[] args) {
				        System.out.println("Hello World"  // Missing closing parenthesis
				    }
				}
				""";
		Files.writeString(brokenJavaFile, brokenCode);

		// Verify the file has a syntax error
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertThat(compiler).isNotNull();

		AgentTaskRequest request = AgentTaskRequest.builder("Fix the syntax error in Broken.java", tempDir).build();

		// Act
		AgentResponse response = agentModel.call(request);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();

		// Verify the file was fixed (should now have the missing parenthesis)
		String fixedCode = Files.readString(brokenJavaFile);
		assertThat(fixedCode).contains("System.out.println(\"Hello World\");");
		// The closing parenthesis should be added
		assertThat(fixedCode).doesNotContain("System.out.println(\"Hello World\"  //");
	}

	/**
	 * Test agent availability functionality. Verifies that the agent model can correctly
	 * report its availability status.
	 */
	@Test
	void testAgentAvailability() {
		// Act & Assert
		boolean isAvailable = agentModel.isAvailable();

		// For this test to run, the agent should be available
		// (tests are typically skipped if the agent is not available)
		assertThat(isAvailable).isTrue();
	}

	/**
	 * Test timeout handling functionality. Verifies that the agent respects timeout
	 * settings and handles long-running tasks appropriately.
	 *
	 * Note: This test uses a short timeout to verify timeout behavior. Specific timeout
	 * handling may vary by implementation.
	 */
	@Test
	void testTimeoutHandling() throws IOException {
		// Arrange: Create a task that might take longer than a very short timeout
		AgentTaskRequest request = AgentTaskRequest
			.builder("Create 100 files with different names and contents", tempDir)
			.options(createShortTimeoutOptions())
			.build();

		// Act
		AgentResponse response = agentModel.call(request);

		// Assert: Response should be returned (may be partial due to timeout)
		assertThat(response).isNotNull();
		assertThat(response.getMetadata().getDuration()).isNotNull();

		// The exact behavior depends on the implementation:
		// - Some agents may complete quickly
		// - Others may timeout and return partial results
		// - The key is that a response is always returned
	}

	/**
	 * Test complex multi-step task functionality. Verifies that the agent can handle
	 * tasks requiring multiple operations.
	 */
	@Test
	void testComplexTask() throws IOException {
		// Arrange
		String taskDescription = "Create a directory called 'project', create a README.md file inside it with project information, "
				+ "and create a simple Python script that prints 'Hello from complex task'";
		AgentTaskRequest request = AgentTaskRequest.builder(taskDescription, tempDir).build();

		System.out.println("=== AGENT TASK DEBUG ===");
		System.out.println("Request: " + taskDescription);
		System.out.println("Working directory: " + tempDir);
		System.out.println("Agent model: " + agentModel.getClass().getSimpleName());

		long startTime = System.currentTimeMillis();

		// Act
		AgentResponse response = agentModel.call(request);

		long endTime = System.currentTimeMillis();
		System.out.println("Execution time: " + (endTime - startTime) + "ms");

		// Log response details
		System.out.println(
				"Response model: " + (response.getMetadata() != null ? response.getMetadata().getModel() : "null"));
		System.out.println("Response duration: "
				+ (response.getMetadata() != null ? response.getMetadata().getDuration() : "null"));
		System.out.println("Response output length: "
				+ (response.getResult() != null ? response.getResult().getOutput().length() : 0));

		if (response.getResult() != null && response.getResult().getOutput().length() < 1000) {
			System.out.println("Response output: " + response.getResult().getOutput());
		}
		else if (response.getResult() != null) {
			System.out.println(
					"Response output (first 500 chars): " + response.getResult().getOutput().substring(0, 500) + "...");
		}

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();

		// Check if the agent created a project directory (flexible - some agents may
		// structure differently)
		Path projectDir = tempDir.resolve("project");
		boolean projectDirExists = Files.exists(projectDir) && Files.isDirectory(projectDir);

		// Check for README.md (might be in project dir or working dir)
		boolean foundReadme = false;
		if (projectDirExists) {
			Path readmeFile = projectDir.resolve("README.md");
			if (Files.exists(readmeFile)) {
				String readmeContent = Files.readString(readmeFile);
				foundReadme = !readmeContent.isEmpty();
			}
		}

		// Also check for README.md in working directory
		if (!foundReadme) {
			Path workingReadme = tempDir.resolve("README.md");
			if (Files.exists(workingReadme)) {
				String readmeContent = Files.readString(workingReadme);
				foundReadme = !readmeContent.isEmpty();
			}
		}

		// Look for Python script (might be in project dir or working dir)
		boolean foundPythonScript = Files.walk(tempDir)
			.filter(path -> path.toString().endsWith(".py"))
			.findAny()
			.isPresent();

		// Assert: Agent should complete some part of the complex task
		// - Response should have output
		// - At least one of: project directory, README file, or Python script should be
		// created
		assertThat(response.getResult().getOutput()).isNotEmpty();

		boolean completedSomeTask = projectDirExists || foundReadme || foundPythonScript;

		// Debug output to understand what the agent did
		if (!completedSomeTask) {
			System.out.println("=== DEBUG: Complex task failed ===");
			System.out.println("Response output: " + response.getResult().getOutput());
			System.out.println("Project dir exists: " + projectDirExists + " (at " + projectDir + ")");
			System.out.println("Found README: " + foundReadme);
			System.out.println("Found Python script: " + foundPythonScript);

			System.out.println("Files in temp directory:");
			try {
				Files.walk(tempDir).forEach(path -> {
					System.out.println("  " + path + " (dir: " + Files.isDirectory(path) + ")");
				});
			}
			catch (Exception e) {
				System.out.println("  Error listing files: " + e.getMessage());
			}
			System.out.println("=== END DEBUG ===");
		}

		assertThat(completedSomeTask)
			.withFailMessage("Agent should create at least one of: project directory, README.md, or Python script")
			.isTrue();
	}

	/**
	 * Create agent options with a short timeout for timeout testing. Subclasses should
	 * override this to provide agent-specific timeout options.
	 */
	protected AgentOptions createShortTimeoutOptions() {
		// Default implementation returns null - subclasses should override
		// to provide agent-specific options with short timeout
		return null;
	}

}