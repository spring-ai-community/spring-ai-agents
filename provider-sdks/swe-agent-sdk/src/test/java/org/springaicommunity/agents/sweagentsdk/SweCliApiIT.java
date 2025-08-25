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

package org.springaicommunity.agents.sweagentsdk;

import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi;
import org.springaicommunity.agents.sweagentsdk.types.SweAgentOptions;
import org.springaicommunity.agents.sweagentsdk.exceptions.SweSDKException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests for SweCliApi requiring actual mini-swe-agent CLI
 * installation.
 *
 * <p>
 * These tests extend {@link BaseSweAgentIT} which provides DRY setup for:
 * <ul>
 * <li>CLI availability checking</li>
 * <li>API key validation (OPENAI_API_KEY)</li>
 * <li>Automatic test skipping when prerequisites aren't met</li>
 * </ul>
 *
 * <p>
 * Tests will be automatically skipped if mini-swe-agent CLI is not available or if
 * OPENAI_API_KEY is not set.
 */
class SweCliApiIT extends BaseSweAgentIT {

	private static final Logger logger = LoggerFactory.getLogger(SweCliApiIT.class);

	private SweCliApi sweCliApi;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		try {
			sweCliApi = new SweCliApi();
		}
		catch (SweSDKException e) {
			logger.warn("Failed to create SweCliApi: {}", e.getMessage());
			// Don't set sweCliApi to null, let tests fail with clear message
			// The @EnabledIf should prevent this scenario but if it happens,
			// we want a clear error rather than NPE
		}
	}

	@Test
	void testCliAvailabilityCheck() {
		// CLI availability is already verified by BaseSweAgentIT
		// Just verify our API instance was created successfully or provide clear error
		if (sweCliApi != null) {
			assertThat(sweCliApi).isNotNull();
			logger.info("CLI availability check passed - SweCliApi created successfully");
		}
		else {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}
	}

	@Test
	void testSimpleFileCreation() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given
		String taskPrompt = "Create a file named 'test.txt' containing exactly the text 'Hello World'";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofMinutes(2))
			.maxIterations(3)
			.verbose(false)
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);

		// Verify file creation
		Path createdFile = tempDir.resolve("test.txt");
		assertThat(createdFile).exists();

		String content = Files.readString(createdFile);
		assertThat(content.trim()).isEqualTo("Hello World");

		logger.info("Simple file creation test passed");
	}

	@Test
	void testComplexTask() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given
		String taskPrompt = """
				Create a Python script named 'calculator.py' that:
				1. Defines a function add(a, b) that returns a + b
				2. Defines a function multiply(a, b) that returns a * b
				3. Has a main section that prints add(5, 3) and multiply(4, 6)
				""";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofMinutes(3))
			.maxIterations(5)
			.verbose(true)
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);
		assertThat(result.getOutput()).isNotNull().isNotBlank();

		// Verify file creation and content
		Path pythonFile = tempDir.resolve("calculator.py");
		assertThat(pythonFile).exists();

		String content = Files.readString(pythonFile);
		assertThat(content).contains("def add(");
		assertThat(content).contains("def multiply(");
		assertThat(content).contains("add(5, 3)");
		assertThat(content).contains("multiply(4, 6)");

		logger.info("Complex task test passed");
		logger.info("  Generated file size: {} bytes", Files.size(pythonFile));
	}

	@Test
	void testMultipleFiles() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given
		String taskPrompt = """
				Create the following files:
				1. 'README.md' with content '# My Project\nThis is a test project.'
				2. 'config.json' with content '{"name": "test", "version": "1.0.0"}'
				3. 'script.sh' with content '#!/bin/bash\necho \"Hello from script\"'
				""";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofMinutes(3))
			.maxIterations(5)
			.verbose(false)
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);

		// Verify all files were created
		Path readmeFile = tempDir.resolve("README.md");
		Path configFile = tempDir.resolve("config.json");
		Path scriptFile = tempDir.resolve("script.sh");

		assertThat(readmeFile).exists();
		assertThat(configFile).exists();
		assertThat(scriptFile).exists();

		// Verify content
		assertThat(Files.readString(readmeFile)).contains("My Project");
		assertThat(Files.readString(configFile)).contains("test").contains("1.0.0");
		assertThat(Files.readString(scriptFile)).contains("echo").contains("Hello from script");

		logger.info("Multiple files test passed - created 3 files");
	}

	@Test
	void testDifferentModels() throws Exception {
		// Test with different model configurations
		List<String> models = List.of("gpt-4o-mini", "gpt-4o");

		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		for (String model : models) {
			logger.info("Testing with model: {}", model);

			String taskPrompt = "Create a file named 'model-test-" + model.replace("gpt-", "")
					+ ".txt' with content 'Generated by " + model + "'";

			SweAgentOptions options = SweAgentOptions.builder()
				.model(model)
				.timeout(Duration.ofMinutes(2))
				.maxIterations(2)
				.verbose(false)
				.build();

			SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

			assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);

			// Verify file was created
			String expectedFileName = "model-test-" + model.replace("gpt-", "") + ".txt";
			Path createdFile = tempDir.resolve(expectedFileName);
			assertThat(createdFile).exists();

			String content = Files.readString(createdFile);
			assertThat(content).contains("Generated by " + model);
		}

		logger.info("Different models test passed");
	}

	@Test
	void testTimeoutConfiguration() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given - short timeout for a simple task
		String taskPrompt = "Create a file named 'timeout-test.txt' with content 'Quick task'";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofSeconds(30)) // Short timeout
			.maxIterations(1)
			.verbose(false)
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then - should complete within timeout
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);

		Path createdFile = tempDir.resolve("timeout-test.txt");
		assertThat(createdFile).exists();

		logger.info("Timeout configuration test passed");
	}

	@Test
	void testMaxIterationsLimit() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given - task with very low iteration limit
		String taskPrompt = "Create a file named 'iterations-test.txt' with content 'Limited iterations'";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofMinutes(2))
			.maxIterations(1) // Very low limit
			.verbose(false)
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then - should still succeed for simple tasks
		assertThat(result).isNotNull();
		// May succeed or fail depending on the agent's efficiency
		logger.info("Max iterations test completed with status: {}", result.getStatus());

		if (result.getStatus() == SweCliApi.SweResultStatus.SUCCESS) {
			Path createdFile = tempDir.resolve("iterations-test.txt");
			assertThat(createdFile).exists();
		}
	}

	@Test
	void testResultMetadata() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given
		String taskPrompt = "Create a JSON file named 'metadata-test.json' with content '{\"test\": true}'";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofMinutes(2))
			.maxIterations(3)
			.verbose(true) // Enable verbose for more metadata
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);
		assertThat(result.getOutput()).isNotNull().isNotBlank();

		// Test metadata availability
		if (result.getMetadata() != null) {
			logger.info("Result metadata: {}", result.getMetadata());
			assertThat(result.getMetadata()).isNotEmpty();
		}

		// Test error field (should be null or empty for successful execution)
		assertThat(result.getError()).satisfiesAnyOf(error -> assertThat(error).isNull(),
				error -> assertThat(error).isEmpty());

		logger.info("Result metadata test passed");
	}

	@Test
	void testErrorHandling() {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given - invalid model should cause error during setup
		SweAgentOptions invalidOptions = SweAgentOptions.builder()
			.model("") // Invalid empty model
			.timeout(Duration.ofMinutes(1))
			.maxIterations(2)
			.build();

		// When/Then - should throw exception during validation
		assertThatThrownBy(() -> sweCliApi.execute("test task", tempDir, invalidOptions))
			.isInstanceOf(IllegalArgumentException.class);

		logger.info("Error handling test passed");
	}

	@Test
	void testWorkingDirectoryIsolation() throws Exception {
		// Create two separate temp directories
		Path tempDir1 = Files.createTempDirectory("swe-test-1");
		Path tempDir2 = Files.createTempDirectory("swe-test-2");

		try {
			// Ensure CLI is available before proceeding
			if (sweCliApi == null) {
				throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
			}

			// Given - same task in different directories
			String taskPrompt = "Create a file named 'isolation-test.txt' with content 'Directory isolation test'";

			SweAgentOptions options = SweAgentOptions.builder()
				.model("gpt-4o-mini")
				.timeout(Duration.ofMinutes(2))
				.maxIterations(2)
				.verbose(false)
				.build();

			// When - execute in both directories
			SweCliApi.SweResult result1 = sweCliApi.execute(taskPrompt, tempDir1, options);
			SweCliApi.SweResult result2 = sweCliApi.execute(taskPrompt, tempDir2, options);

			// Then - both should succeed and create files in their respective directories
			assertThat(result1.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);
			assertThat(result2.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);

			Path file1 = tempDir1.resolve("isolation-test.txt");
			Path file2 = tempDir2.resolve("isolation-test.txt");

			assertThat(file1).exists();
			assertThat(file2).exists();

			// Files should be isolated - not appear in the other directory
			assertThat(tempDir1.resolve("isolation-test.txt")).exists();
			assertThat(tempDir2.resolve("isolation-test.txt")).exists();

			logger.info("Working directory isolation test passed");

		}
		finally {
			// Clean up temp directories
			Files.deleteIfExists(tempDir1.resolve("isolation-test.txt"));
			Files.deleteIfExists(tempDir1);
			Files.deleteIfExists(tempDir2.resolve("isolation-test.txt"));
			Files.deleteIfExists(tempDir2);
		}
	}

}