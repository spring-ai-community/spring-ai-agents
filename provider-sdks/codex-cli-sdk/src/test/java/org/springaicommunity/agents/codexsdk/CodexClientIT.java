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

package org.springaicommunity.agents.codexsdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.codexsdk.types.ExecuteOptions;
import org.springaicommunity.agents.codexsdk.types.ExecuteResult;
import org.zeroturnaround.exec.ProcessExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link CodexClient} that require Codex CLI to be installed.
 *
 * <p>
 * Authentication can be provided via:
 * <ul>
 * <li>ChatGPT session: Run `codex login` (recommended)</li>
 * <li>API key: Run `echo $OPENAI_API_KEY | codex login --with-api-key`</li>
 * <li>Exec override: Set CODEX_API_KEY environment variable (exec mode only)</li>
 * </ul>
 *
 * @author Spring AI Community
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
		disabledReason = "Codex CLI not available in CI environment")
class CodexClientIT {

	private CodexClient client;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() throws Exception {
		// Initialize git repository (Codex requires this)
		new ProcessExecutor().command("git", "init").directory(tempDir.toFile()).execute();

		ExecuteOptions options = ExecuteOptions.builder()
			.fullAuto(true)
			.timeout(Duration.ofMinutes(3))
			.skipGitCheck(false) // Keep git check enabled for safety
			.build();

		client = CodexClient.create(options, tempDir);

		// Verify Codex CLI is available before running tests
		assumeTrue(client.isAvailable(), "Codex CLI must be available for integration tests");
	}

	@Test
	void testSimpleFileCreation() throws Exception {
		// Act
		ExecuteResult result = client.execute("Create a file named test.txt with content 'Hello from Codex'");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getExitCode()).isEqualTo(0);
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.getOutput()).isNotNull();
		assertThat(result.getDuration()).isNotNull();
		assertThat(result.getModel()).isNotNull();
		assertThat(result.getSessionId()).isNotNull(); // Codex provides session ID

		// Verify file was created
		Path testFile = tempDir.resolve("test.txt");
		assertThat(Files.exists(testFile)).isTrue();
		String content = Files.readString(testFile);
		assertThat(content).contains("Hello from Codex");
	}

	@Test
	void testFileListAndRead() throws Exception {
		// Arrange: Create a test file
		Path sampleFile = tempDir.resolve("sample.txt");
		Files.writeString(sampleFile, "Sample content for Codex");

		// Act
		ExecuteResult result = client.execute("List all files in the current directory");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();
		// Activity log contains the full execution trace including file list
		assertThat(result.getActivityLog()).containsIgnoringCase("sample.txt");
	}

	@Test
	void testSessionResume() throws Exception {
		// First execution
		ExecuteResult firstResult = client.execute("Create a file named first.txt with content 'First task'");
		assertThat(firstResult.isSuccessful()).isTrue();
		assertThat(firstResult.getSessionId()).isNotNull();

		String sessionId = firstResult.getSessionId();

		// Resume session with new task
		ExecuteResult resumeResult = client.resume(sessionId,
				"Create a file named second.txt with content 'Second task'");

		// Assert
		assertThat(resumeResult.isSuccessful()).isTrue();
		assertThat(resumeResult.getSessionId()).isEqualTo(sessionId);

		// Verify both files exist
		assertThat(Files.exists(tempDir.resolve("first.txt"))).isTrue();
		assertThat(Files.exists(tempDir.resolve("second.txt"))).isTrue();
	}

	@Test
	void testAvailabilityCheck() {
		boolean available = client.isAvailable();
		assertThat(available).isTrue();
	}

	@Test
	void testCliPathDiscovery() {
		String codexPath = client.getCodexCliPath();
		assertThat(codexPath).isNotNull();
		assertThat(codexPath).isNotEmpty();
		assertThat(codexPath).contains("codex");
	}

	@Test
	void testActivityLogCapture() {
		ExecuteResult result = client.execute("List files");

		assertThat(result.getActivityLog()).isNotNull();
		assertThat(result.getActivityLog()).isNotEmpty();
		// Activity log should contain execution details
		assertThat(result.getActivityLog()).containsAnyOf("workdir:", "model:", "sandbox:");
	}

}
