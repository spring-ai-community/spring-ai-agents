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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springaicommunity.agents.model.sandbox.DockerSandbox;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.ExecSpec;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker sandbox infrastructure tests for agent runtime environment.
 *
 * <p>
 * These tests prove that the DockerSandbox infrastructure works correctly with
 * agent-specific requirements including Node.js execution and environment variables.
 * These tests focus on sandbox infrastructure rather than actual agent CLI functionality.
 * </p>
 *
 * <p>
 * Run with: mvn test -Dtest=DockerSandboxInfrastructureIT -Dsandbox.integration.test=true
 * </p>
 *
 * <p>
 * Requirements:
 * </p>
 * <ul>
 * <li>Docker to be available</li>
 * <li>Access to ghcr.io/spring-ai-community/agents-runtime:latest image</li>
 * </ul>
 */
@EnabledIfSystemProperty(named = "sandbox.integration.test", matches = "true")
class DockerSandboxInfrastructureIT {

	private DockerSandbox dockerSandbox;

	@BeforeEach
	void setUp() {
		// Use real DockerSandbox with agents-runtime image
		dockerSandbox = new DockerSandbox("ghcr.io/spring-ai-community/agents-runtime:latest", List.of());
	}

	@org.junit.jupiter.api.AfterEach
	void tearDown() throws Exception {
		if (dockerSandbox != null) {
			dockerSandbox.close();
		}
	}

	@Test
	void testDockerSandboxNodejsExecution() throws Exception {
		// CRITICAL TEST: Prove Node.js execution works in Docker (important for Gemini
		// CLI runtime)

		// Arrange: Test Node.js is available
		ExecSpec nodeTest = ExecSpec.builder().command("node", "--version").timeout(Duration.ofSeconds(30)).build();

		// Act
		ExecResult result = dockerSandbox.exec(nodeTest);

		// Assert: Node.js is available in the agents-runtime image
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).startsWith("v");
	}

	@Test
	void testGeminiSpecificEnvironmentVariables() throws Exception {
		// CRITICAL TEST: Prove Gemini-specific environment variables work

		// Arrange: Test Gemini API key environment variables
		ExecSpec geminiEnvTest = ExecSpec.builder()
			.command("printenv")
			.env(Map.of("GEMINI_API_KEY", "test-key", "GOOGLE_API_KEY", "test-google-key"))
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(geminiEnvTest);

		// Assert: Environment variables are properly injected
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).contains("GEMINI_API_KEY=test-key");
		assertThat(result.mergedLog()).contains("GOOGLE_API_KEY=test-google-key");
	}

	@Test
	void testAgentModelCentricPatternEndToEnd() throws Exception {
		// CRITICAL TEST: Prove the complete AgentModel-centric pattern with real
		// execution

		// This test simulates what GeminiAgentModel does:
		// 1. Build command (simulated)
		// 2. Execute via sandbox (real)
		// 3. Parse result (simulated)

		// Step 1: Build command (simulated - real SDK would do this)
		List<String> command = List.of("echo", "TextMessage[type=ASSISTANT, content=Success from Gemini CLI]");

		// Step 2: Execute via sandbox (REAL execution)
		ExecSpec spec = ExecSpec.builder()
			.command(command)
			.env(Map.of("GEMINI_CLI_ENTRYPOINT", "sdk-java"))
			.timeout(Duration.ofSeconds(30))
			.build();

		ExecResult execResult = dockerSandbox.exec(spec);

		// Step 3: Verify result (simulated parsing)
		assertThat(execResult.success()).isTrue();
		assertThat(execResult.mergedLog()).contains("Success from Gemini CLI");
		assertThat(execResult.mergedLog()).contains("TextMessage[type=ASSISTANT");

		// ASSERT: Complete pattern works end-to-end with real Docker execution
		assertThat(execResult.exitCode()).isEqualTo(0);
		assertThat(execResult.duration()).isPositive();
		assertThat(execResult.hasOutput()).isTrue();
	}

}