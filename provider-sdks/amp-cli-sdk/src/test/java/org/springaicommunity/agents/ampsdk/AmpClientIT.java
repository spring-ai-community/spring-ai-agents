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

package org.springaicommunity.agents.ampsdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.ampsdk.types.ExecuteOptions;
import org.springaicommunity.agents.ampsdk.types.ExecuteResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link AmpClient} that require Amp CLI to be installed.
 *
 * <p>
 * Authentication can be provided via:
 * <ul>
 * <li>Session authentication: Run `amp login` (recommended)</li>
 * <li>API key: Set AMP_API_KEY environment variable</li>
 * </ul>
 *
 * @author Spring AI Community
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
		disabledReason = "Amp CLI not available in CI environment")
class AmpClientIT {

	private AmpClient client;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		ExecuteOptions options = ExecuteOptions.builder()
			.dangerouslyAllowAll(true)
			.timeout(Duration.ofMinutes(3))
			.build();

		client = AmpClient.create(options, tempDir);

		// Verify Amp CLI is available before running tests
		assumeTrue(client.isAvailable(), "Amp CLI must be available for integration tests");
	}

	@Test
	void testSimpleFileCreation() throws Exception {
		// Act
		ExecuteResult result = client.execute("Create a file named test.txt with content 'Hello from Amp'");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getExitCode()).isEqualTo(0);
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.getOutput()).isNotNull();
		assertThat(result.getDuration()).isNotNull();

		// Verify file was created
		Path testFile = tempDir.resolve("test.txt");
		assertThat(Files.exists(testFile)).isTrue();
		String content = Files.readString(testFile);
		assertThat(content).contains("Hello from Amp");
	}

	@Test
	void testFileListAndRead() throws Exception {
		// Arrange: Create a test file
		Path sampleFile = tempDir.resolve("sample.txt");
		Files.writeString(sampleFile, "Sample content");

		// Act
		ExecuteResult result = client.execute("List all files in the current directory and show their contents");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.getOutput()).containsIgnoringCase("sample.txt");
	}

	@Test
	void testAvailabilityCheck() {
		boolean available = client.isAvailable();
		assertThat(available).isTrue();
	}

	@Test
	void testCliPathDiscovery() {
		String ampPath = client.getAmpCliPath();
		assertThat(ampPath).isNotNull();
		assertThat(ampPath).isNotEmpty();
	}

}
