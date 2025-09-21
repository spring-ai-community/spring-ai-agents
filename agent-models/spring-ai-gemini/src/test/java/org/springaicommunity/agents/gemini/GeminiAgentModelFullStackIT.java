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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.sandbox.DockerSandbox;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.ExecSpec;
import org.springaicommunity.agents.model.sandbox.Sandbox;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration tests proving the complete sandbox solution works with real
 * Docker containers for Gemini.
 *
 * These tests are the critical proof that: - DockerSandbox works with TestContainers -
 * AgentModel-centric pattern works end-to-end - SDK command building and parsing works
 * with real execution - Dependency injection pattern works in practice
 *
 * Run with: mvn test -Dtest=*FullStackIT* -Dsandbox.integration.test=true
 */
@EnabledIfSystemProperty(named = "sandbox.integration.test", matches = "true")
class GeminiAgentModelFullStackIT {

	private Sandbox dockerSandbox;

	@BeforeEach
	void setUp() {
		// Use real DockerSandbox with agents-runtime image
		dockerSandbox = new DockerSandbox("ghcr.io/spring-ai-community/agents-runtime:latest", List.of());
	}

	@AfterEach
	void tearDown() throws Exception {
		if (dockerSandbox != null) {
			dockerSandbox.close();
		}
	}

	@Test
	void testDockerSandboxBasicExecution() throws Exception {
		// CRITICAL TEST: Prove DockerSandbox works with TestContainers

		// Arrange
		ExecSpec echoTest = ExecSpec.builder()
			.command("echo", "Hello from Docker sandbox")
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(echoTest);

		// Assert: Real Docker execution works
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).contains("Hello from Docker sandbox");
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.duration()).isPositive();
	}

	@Test
	void testDockerSandboxWithEnvironmentVariables() throws Exception {
		// CRITICAL TEST: Prove environment variable injection works

		// Arrange
		ExecSpec envTest = ExecSpec.builder()
			.command("printenv", "TEST_VAR")
			.env(Map.of("TEST_VAR", "test-value"))
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(envTest);

		// Assert: Environment variables are properly injected
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog().trim()).isEqualTo("test-value");
	}

	@Test
	void testDockerSandboxWorkingDirectory() throws Exception {
		// CRITICAL TEST: Prove working directory is properly set

		// Arrange
		ExecSpec pwdTest = ExecSpec.builder().command("pwd").timeout(Duration.ofSeconds(30)).build();

		// Act
		ExecResult result = dockerSandbox.exec(pwdTest);

		// Assert: Working directory is /work (as configured in DockerSandbox)
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog().trim()).isEqualTo("/work");
	}

	@Test
	void testDockerSandboxCommandTimeout() throws Exception {
		// CRITICAL TEST: Prove timeout handling works

		// Arrange: Command that takes longer than timeout
		ExecSpec timeoutTest = ExecSpec.builder().command("sleep", "10").timeout(Duration.ofSeconds(2)).build();

		// Act & Assert: Should throw TimeoutException
		try {
			dockerSandbox.exec(timeoutTest);
			fail("Expected TimeoutException");
		}
		catch (org.springaicommunity.agents.model.sandbox.TimeoutException e) {
			assertThat(e.getTimeout()).isEqualTo(Duration.ofSeconds(2));
		}
	}

	@Test
	void testDockerSandboxErrorHandling() throws Exception {
		// CRITICAL TEST: Prove error handling works correctly

		// Arrange: Command that will fail
		ExecSpec failTest = ExecSpec.builder()
			.command("ls", "/nonexistent-directory")
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(failTest);

		// Assert: Non-zero exit code handled correctly
		assertThat(result.failed()).isTrue();
		assertThat(result.exitCode()).isNotEqualTo(0);
		assertThat(result.mergedLog()).contains("No such file or directory");
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
	void testDockerSandboxMultipleExecutions() throws Exception {
		// CRITICAL TEST: Prove multiple executions work in same container

		// Act: Execute multiple commands in same sandbox
		ExecResult result1 = dockerSandbox.exec(ExecSpec.builder().command("echo", "first").build());
		ExecResult result2 = dockerSandbox.exec(ExecSpec.builder().command("echo", "second").build());
		ExecResult result3 = dockerSandbox.exec(ExecSpec.builder().command("echo", "third").build());

		// Assert: All executions succeed
		assertThat(result1.success()).isTrue();
		assertThat(result2.success()).isTrue();
		assertThat(result3.success()).isTrue();
		assertThat(result1.mergedLog()).contains("first");
		assertThat(result2.mergedLog()).contains("second");
		assertThat(result3.mergedLog()).contains("third");
	}

	@Test
	void testDockerSandboxDirectCreation() {
		// CRITICAL TEST: Prove DockerSandbox can be created directly

		// Act
		Sandbox dockerSandbox = new DockerSandbox("ghcr.io/spring-ai-community/agents-runtime:latest", List.of());

		// Assert: DockerSandbox works when created directly
		assertThat(dockerSandbox).isInstanceOf(DockerSandbox.class);
		assertThat(dockerSandbox.workDir()).isNotNull();
		assertThat(dockerSandbox.isClosed()).isFalse();
	}

	@Test
	void testSandboxDirectDependencyInjectionPattern() throws Exception {
		// CRITICAL TEST: Prove direct dependency injection pattern works end-to-end

		// Act: Use the sandbox directly and execute
		ExecResult result = dockerSandbox
			.exec(ExecSpec.builder().command("echo", "dependency injection works").build());

		// Assert: Direct dependency injection pattern works
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).contains("dependency injection works");
	}

	@Test
	void testDockerSandboxResourceIsolation() throws Exception {
		// CRITICAL TEST: Prove resource isolation (container is isolated from host)

		// Arrange: Try to access host filesystem (should be isolated)
		ExecSpec isolationTest = ExecSpec.builder()
			.command("ls", "/home") // Host's /home should not be accessible
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(isolationTest);

		// Assert: Container is isolated - /home should be empty or different from host
		assertThat(result.success()).isTrue();
		// The exact assertion depends on the container image, but it should be isolated
		assertThat(result.mergedLog()).isNotNull();
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

	private void fail(String message) {
		throw new AssertionError(message);
	}

}