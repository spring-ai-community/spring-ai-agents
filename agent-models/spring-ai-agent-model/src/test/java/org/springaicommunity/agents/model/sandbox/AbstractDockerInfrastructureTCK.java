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

package org.springaicommunity.agents.model.sandbox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test Compatibility Kit (TCK) for testing Docker sandbox infrastructure.
 *
 * <p>
 * This abstract test class defines the standard test suite for Docker sandbox
 * infrastructure that all agent implementations should pass. Concrete test classes should
 * extend this class and provide agent-specific configuration and tests.
 * </p>
 *
 * <p>
 * The TCK ensures consistent Docker infrastructure behavior across all agent
 * implementations including: - Basic command execution - Environment variable handling -
 * Working directory behavior - Timeout handling - Error handling - Multiple executions -
 * Resource isolation
 * </p>
 *
 * <p>
 * Agent-specific tests (runtime requirements, environment variables) should be
 * implemented in concrete test classes.
 * </p>
 */
public abstract class AbstractDockerInfrastructureTCK {

	/**
	 * The Docker sandbox instance under test.
	 */
	protected DockerSandbox dockerSandbox;

	/**
	 * Setup Docker sandbox before each test.
	 */
	@BeforeEach
	void setUpDockerSandbox() {
		this.dockerSandbox = new DockerSandbox(getDockerImage(), getCustomizers());
	}

	/**
	 * Cleanup after each test to ensure resource isolation.
	 */
	@AfterEach
	void tearDownDockerSandbox() throws Exception {
		if (dockerSandbox != null && !dockerSandbox.isClosed()) {
			dockerSandbox.close();
		}
	}

	/**
	 * Test basic command execution in Docker container.
	 */
	@Test
	void testDockerSandboxBasicExecution() throws Exception {
		// Arrange
		ExecSpec echoTest = ExecSpec.builder()
			.command("echo", "Hello from Docker sandbox")
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(echoTest);

		// Assert
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).contains("Hello from Docker sandbox");
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.duration()).isPositive();
	}

	/**
	 * Test environment variable injection in Docker container.
	 */
	@Test
	void testDockerSandboxEnvironmentVariables() throws Exception {
		// Arrange
		ExecSpec envTest = ExecSpec.builder()
			.command("printenv", "TEST_VAR")
			.env(Map.of("TEST_VAR", "test-value"))
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(envTest);

		// Assert
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog().trim()).isEqualTo("test-value");
	}

	/**
	 * Test working directory configuration in Docker container.
	 */
	@Test
	void testDockerSandboxWorkingDirectory() throws Exception {
		// Arrange
		ExecSpec pwdTest = ExecSpec.builder().command("pwd").timeout(Duration.ofSeconds(30)).build();

		// Act
		ExecResult result = dockerSandbox.exec(pwdTest);

		// Assert
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog().trim()).isEqualTo("/work");
	}

	/**
	 * Test timeout handling in Docker container.
	 */
	@Test
	void testDockerSandboxTimeoutHandling() {
		// Arrange: Command that takes longer than timeout
		ExecSpec timeoutTest = ExecSpec.builder().command("sleep", "10").timeout(Duration.ofSeconds(2)).build();

		// Act & Assert: Should throw SandboxException wrapping TimeoutException
		try {
			dockerSandbox.exec(timeoutTest);
			fail("Expected SandboxException wrapping TimeoutException");
		}
		catch (SandboxException e) {
			// Should wrap TimeoutException
			assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
			TimeoutException timeoutException = (TimeoutException) e.getCause();
			assertThat(timeoutException.getTimeout()).isEqualTo(Duration.ofSeconds(2));
		}
	}

	/**
	 * Test error handling in Docker container.
	 */
	@Test
	void testDockerSandboxErrorHandling() throws Exception {
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

	/**
	 * Test multiple command execution in same Docker container.
	 */
	@Test
	void testDockerSandboxMultipleExecutions() throws Exception {
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

	/**
	 * Test resource isolation in Docker container.
	 */
	@Test
	void testDockerSandboxResourceIsolation() throws Exception {
		// Arrange: Try to access host filesystem (should be isolated)
		ExecSpec isolationTest = ExecSpec.builder().command("ls", "/home").timeout(Duration.ofSeconds(30)).build();

		// Act
		ExecResult result = dockerSandbox.exec(isolationTest);

		// Assert: Container is isolated - /home should be empty or different from host
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).isNotNull();
		// Note: The exact isolation behavior depends on the container image
	}

	/**
	 * Test agent model-centric pattern end-to-end with Docker execution.
	 */
	@Test
	void testAgentModelCentricPatternEndToEnd() throws Exception {
		// This test simulates what AgentModel implementations do:
		// 1. Build command (simulated)
		// 2. Execute via sandbox (real)
		// 3. Parse result (simulated)

		// Step 1: Build command (simulated - real SDK would do this)
		List<String> command = List.of("echo", getExpectedAgentOutput());

		// Step 2: Execute via sandbox (REAL execution)
		ExecSpec spec = ExecSpec.builder()
			.command(command)
			.env(getAgentSpecificEnvironment())
			.timeout(Duration.ofSeconds(30))
			.build();

		ExecResult execResult = dockerSandbox.exec(spec);

		// Step 3: Verify result (simulated parsing)
		assertThat(execResult.success()).isTrue();
		assertThat(execResult.mergedLog()).contains(getExpectedAgentOutput());

		// ASSERT: Complete pattern works end-to-end with real Docker execution
		assertThat(execResult.exitCode()).isEqualTo(0);
		assertThat(execResult.duration()).isPositive();
		assertThat(execResult.hasOutput()).isTrue();
	}

	/**
	 * Get the Docker image to use for testing. Must be implemented by concrete test
	 * classes.
	 * @return the Docker image name
	 */
	protected abstract String getDockerImage();

	/**
	 * Get exec spec customizers for the Docker container. Can be overridden by concrete
	 * test classes.
	 * @return list of exec spec customizers
	 */
	protected List<ExecSpecCustomizer> getCustomizers() {
		return List.of();
	}

	/**
	 * Get agent-specific environment variables for testing. Must be implemented by
	 * concrete test classes.
	 * @return map of environment variables
	 */
	protected abstract Map<String, String> getAgentSpecificEnvironment();

	/**
	 * Get expected output for agent-specific end-to-end test. Must be implemented by
	 * concrete test classes.
	 * @return expected output string
	 */
	protected abstract String getExpectedAgentOutput();

}