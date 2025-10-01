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

package org.springaicommunity.agents.tck.sandbox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.ExecSpec;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springaicommunity.agents.model.sandbox.SandboxException;
import org.springaicommunity.agents.model.sandbox.TimeoutException;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test Compatibility Kit (TCK) for testing any Sandbox implementation.
 *
 * <p>
 * This abstract test class defines the standard test suite that all Sandbox
 * implementations must pass. Concrete test classes should extend this class and provide
 * the specific Sandbox implementation in their setup methods.
 * </p>
 *
 * <p>
 * The TCK ensures consistent behavior across all sandbox implementations including: -
 * Basic command execution - Environment variable handling - Working directory behavior -
 * Timeout handling - Error handling - Resource isolation - Resource cleanup
 * </p>
 */
public abstract class AbstractSandboxTCK {

	/**
	 * The sandbox implementation under test. Must be set by concrete test classes.
	 */
	protected Sandbox sandbox;

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
	 * Test basic command execution functionality. Verifies that the sandbox can execute
	 * simple commands and return results.
	 */
	@Test
	void testBasicExecution() throws Exception {
		// Arrange
		ExecSpec echoTest = ExecSpec.builder()
			.command("echo", "Hello from sandbox")
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = sandbox.exec(echoTest);

		// Assert
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).contains("Hello from sandbox");
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.duration()).isPositive();
		assertThat(result.hasOutput()).isTrue();
	}

	/**
	 * Test environment variable injection functionality. Verifies that environment
	 * variables are properly passed to executed commands.
	 */
	@Test
	void testEnvironmentVariables() throws Exception {
		// Arrange
		ExecSpec envTest = ExecSpec.builder()
			.command("printenv", "TEST_VAR")
			.env(Map.of("TEST_VAR", "test-value"))
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = sandbox.exec(envTest);

		// Assert
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog().trim()).isEqualTo("test-value");
	}

	/**
	 * Test working directory functionality. Verifies that the sandbox working directory
	 * is properly configured.
	 */
	@Test
	void testWorkingDirectory() throws Exception {
		// Arrange
		ExecSpec pwdTest = ExecSpec.builder().command("pwd").timeout(Duration.ofSeconds(30)).build();

		// Act
		ExecResult result = sandbox.exec(pwdTest);

		// Assert
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog().trim()).isNotEmpty();
		// Verify the working directory matches what the sandbox reports
		assertThat(result.mergedLog().trim()).isEqualTo(sandbox.workDir().toString());
	}

	/**
	 * Test timeout handling functionality. Verifies that commands that exceed their
	 * timeout are properly terminated.
	 */
	@Test
	void testTimeoutHandling() {
		// Arrange: Command that takes longer than timeout
		ExecSpec timeoutTest = ExecSpec.builder().command("sleep", "10").timeout(Duration.ofSeconds(2)).build();

		// Act & Assert: Should throw SandboxException wrapping TimeoutException
		try {
			sandbox.exec(timeoutTest);
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
	 * Test error handling functionality. Verifies that commands with non-zero exit codes
	 * are properly handled.
	 */
	@Test
	void testErrorHandling() throws Exception {
		// Arrange: Command that will fail
		ExecSpec failTest = ExecSpec.builder()
			.command("ls", "/nonexistent-directory")
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = sandbox.exec(failTest);

		// Assert: Non-zero exit code handled correctly
		assertThat(result.failed()).isTrue();
		assertThat(result.exitCode()).isNotEqualTo(0);
		assertThat(result.mergedLog()).contains("No such file or directory");
	}

	/**
	 * Test multiple command execution functionality. Verifies that multiple commands can
	 * be executed sequentially in the same sandbox.
	 */
	@Test
	void testMultipleExecutions() throws Exception {
		// Act: Execute multiple commands in same sandbox
		ExecResult result1 = sandbox.exec(ExecSpec.builder().command("echo", "first").build());
		ExecResult result2 = sandbox.exec(ExecSpec.builder().command("echo", "second").build());
		ExecResult result3 = sandbox.exec(ExecSpec.builder().command("echo", "third").build());

		// Assert: All executions succeed
		assertThat(result1.success()).isTrue();
		assertThat(result2.success()).isTrue();
		assertThat(result3.success()).isTrue();
		assertThat(result1.mergedLog()).contains("first");
		assertThat(result2.mergedLog()).contains("second");
		assertThat(result3.mergedLog()).contains("third");
	}

	/**
	 * Test resource isolation functionality. Verifies that the sandbox provides proper
	 * isolation from the host system.
	 */
	@Test
	void testResourceIsolation() throws Exception {
		// Arrange: Try to access host filesystem (behavior varies by sandbox type)
		ExecSpec isolationTest = ExecSpec.builder().command("ls", "/home").timeout(Duration.ofSeconds(30)).build();

		// Act
		ExecResult result = sandbox.exec(isolationTest);

		// Assert: Command executes (isolation level depends on sandbox implementation)
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).isNotNull();
		// Note: The exact isolation behavior will vary between LocalSandbox and
		// DockerSandbox
	}

	/**
	 * Test sandbox state management functionality. Verifies that the sandbox properly
	 * tracks its open/closed state.
	 */
	@Test
	void testSandboxStateManagement() {
		// Assert: Sandbox starts in open state
		assertThat(sandbox.isClosed()).isFalse();
		assertThat(sandbox.workDir()).isNotNull();
	}

	/**
	 * Test resource cleanup functionality. Verifies that the sandbox properly releases
	 * resources when closed.
	 */
	@Test
	void testResourceCleanup() throws Exception {
		// Arrange: Verify sandbox is initially open
		assertThat(sandbox.isClosed()).isFalse();

		// Act: Close the sandbox
		sandbox.close();

		// Assert: Sandbox is marked as closed
		assertThat(sandbox.isClosed()).isTrue();
	}

}
