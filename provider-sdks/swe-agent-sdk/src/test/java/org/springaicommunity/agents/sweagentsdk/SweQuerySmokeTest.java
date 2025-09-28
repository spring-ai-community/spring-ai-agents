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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight integration tests for basic SWE Agent query functionality.
 *
 * <p>
 * This test extends {@link BaseSweAgentIT} which automatically discovers SWE Agent CLI
 * and ensures all tests fail gracefully with a clear message if SWE Agent CLI is not
 * available or API keys are missing.
 * </p>
 *
 * <p>
 * This is the SWE Agent equivalent of {@code QuerySmokeTest.java} for Claude Code SDK,
 * containing only the 3 most basic integration tests for smoke testing.
 * </p>
 */
class SweQuerySmokeTest extends BaseSweAgentIT {

	private static final Logger logger = LoggerFactory.getLogger(SweQuerySmokeTest.class);

	private SweCliApi sweCliApi;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		try {
			sweCliApi = new SweCliApi();
		}
		catch (Exception e) {
			logger.warn("Failed to create SweCliApi: {}", e.getMessage());
			// Don't set sweCliApi to null, let tests fail with clear message
			// The @EnabledIf should prevent this scenario but if it happens,
			// we want a clear error rather than NPE
		}
	}

	@Test
	@Disabled("SWE Agent CLI prerequisites not available in CI")
	void testBasicQuery() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given
		String taskPrompt = "Create a text file named 'hello.txt' with the content 'Hello, SWE Agent!'";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofSeconds(30))
			.maxIterations(3)
			.verbose(false)
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);

		// Verify the file was created
		Path createdFile = tempDir.resolve("hello.txt");
		assertThat(createdFile).exists();

		String content = Files.readString(createdFile);
		assertThat(content.trim()).isEqualTo("Hello, SWE Agent!");
	}

	@Test
	@Disabled("SWE Agent CLI prerequisites not available in CI")
	void testQueryWithOptions() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given
		String taskPrompt = "Create a JSON file named 'config.json' with content: {\"version\": \"1.0\", \"name\": \"test\"}";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofSeconds(30))
			.maxIterations(2)
			.verbose(true)
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);
		assertThat(result.getOutput()).isNotNull();

		// Verify the file was created
		Path createdFile = tempDir.resolve("config.json");
		assertThat(createdFile).exists();

		String content = Files.readString(createdFile);
		assertThat(content).contains("version");
		assertThat(content).contains("1.0");
		assertThat(content).contains("name");
		assertThat(content).contains("test");
	}

	@Test
	@Disabled("SWE Agent CLI prerequisites not available in CI")
	void testQueryResultAnalysis() throws Exception {
		// Ensure CLI is available before proceeding
		if (sweCliApi == null) {
			throw new IllegalStateException("SweCliApi not available - integration test prerequisites not met");
		}

		// Given
		String taskPrompt = "Create a Python file named 'simple.py' that prints 'Testing SWE Agent'";

		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofSeconds(30))
			.maxIterations(2)
			.verbose(false)
			.build();

		// When
		SweCliApi.SweResult result = sweCliApi.execute(taskPrompt, tempDir, options);

		// Then
		// Test basic result structure
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);
		assertThat(result.getOutput()).isNotNull();
		assertThat(result.getError()).satisfiesAnyOf(error -> assertThat(error).isNull(),
				error -> assertThat(error).isEmpty(),
				error -> assertThat(error).isEqualTo("Warning: Input is not a terminal (fd=0)."));

		// Test file creation
		Path createdFile = tempDir.resolve("simple.py");
		assertThat(createdFile).exists();

		String content = Files.readString(createdFile);
		assertThat(content).contains("Testing SWE Agent");
		assertThat(content).contains("print");

		// Test metadata (if available)
		if (result.getMetadata() != null) {
			logger.info("Task execution metadata: {}", result.getMetadata());
		}
	}

}