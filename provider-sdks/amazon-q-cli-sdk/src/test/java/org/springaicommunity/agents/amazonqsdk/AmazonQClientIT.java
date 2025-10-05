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

package org.springaicommunity.agents.amazonqsdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteOptions;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for AmazonQClient.
 *
 * @author Spring AI Community
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
		disabledReason = "Amazon Q CLI authentication not available in CI environment")
class AmazonQClientIT {

	@TempDir
	Path tempDir;

	private AmazonQClient client;

	@BeforeEach
	void setUp() {
		client = AmazonQClient.create(tempDir);

		// Skip tests if Amazon Q CLI is not available
		assumeTrue(client.isAvailable(), "Amazon Q CLI must be available for integration tests");
	}

	@Test
	void testSimpleFileCreation() throws Exception {
		// Arrange
		ExecuteOptions options = ExecuteOptions.builder()
			.trustAllTools(true)
			.noInteractive(true)
			.timeout(Duration.ofMinutes(3))
			.build();

		// Act
		ExecuteResult result = client.execute("Create a file named 'hello.txt' with content 'Hello World!'", options);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();

		// Verify the file was created
		Path helloFile = tempDir.resolve("hello.txt");
		assertThat(Files.exists(helloFile)).isTrue();

		String content = Files.readString(helloFile);
		assertThat(content.trim()).isEqualTo("Hello World!");
	}

	@Test
	void testFileListAndRead() throws Exception {
		// Arrange: Create a test file
		Path testFile = tempDir.resolve("sample.txt");
		Files.writeString(testFile, "Sample content");

		ExecuteOptions options = ExecuteOptions.builder()
			.trustAllTools(true)
			.noInteractive(true)
			.timeout(Duration.ofMinutes(3))
			.build();

		// Act
		ExecuteResult result = client.execute("List all files in the current directory and read sample.txt", options);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.getOutput()).containsIgnoringCase("sample.txt");
	}

	@Test
	void testExecuteWithVerboseLogging() {
		// Arrange
		ExecuteOptions options = ExecuteOptions.builder()
			.trustAllTools(true)
			.noInteractive(true)
			.verbose(true)
			.timeout(Duration.ofMinutes(2))
			.build();

		// Act
		ExecuteResult result = client.execute("What files are in the current directory?", options);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();
	}

}
