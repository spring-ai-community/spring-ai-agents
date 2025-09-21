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

package org.springaicommunity.agents.sweagent;

import org.junit.jupiter.api.AfterEach;
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
 * Docker sandbox infrastructure tests for SWE agent runtime environment.
 *
 * <p>
 * These tests prove that the DockerSandbox infrastructure works correctly with
 * SWE-agent-specific requirements including Python execution and environment variables.
 * These tests focus on sandbox infrastructure rather than actual SWE agent CLI
 * functionality.
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

	@AfterEach
	void tearDown() throws Exception {
		if (dockerSandbox != null) {
			dockerSandbox.close();
		}
	}

	@Test
	void testDockerSandboxPythonExecution() throws Exception {
		// CRITICAL TEST: Prove Python execution works in Docker (important for SWE
		// agent runtime)

		// Arrange: Test Python is available
		ExecSpec pythonTest = ExecSpec.builder()
			.command("python3", "--version")
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(pythonTest);

		// Assert: Python is available in the agents-runtime image
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).containsIgnoringCase("python");
	}

	@Test
	void testSweAgentSpecificEnvironmentVariables() throws Exception {
		// CRITICAL TEST: Prove SWE agent-specific environment variables work

		// Arrange: Test SWE agent environment variables
		ExecSpec sweEnvTest = ExecSpec.builder()
			.command("printenv")
			.env(Map.of("OPENAI_API_KEY", "test-key", "SWE_AGENT_CONFIG", "test-config"))
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(sweEnvTest);

		// Assert: Environment variables are properly injected
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).contains("OPENAI_API_KEY=test-key");
		assertThat(result.mergedLog()).contains("SWE_AGENT_CONFIG=test-config");
	}

	@Test
	void testAgentModelCentricPatternEndToEnd() throws Exception {
		// CRITICAL TEST: Prove the complete AgentModel-centric pattern with real
		// execution

		// This test simulates what SweAgentModel would do:
		// 1. Build command (simulated)
		// 2. Execute via sandbox (real)
		// 3. Parse result (simulated)

		// Step 1: Build command (simulated - real SDK would do this)
		List<String> command = List.of("echo", "{\"status\": \"success\", \"message\": \"SWE Agent execution\"}");

		// Step 2: Execute via sandbox (REAL execution)
		ExecSpec spec = ExecSpec.builder()
			.command(command)
			.env(Map.of("SWE_AGENT_ENTRYPOINT", "sdk-java"))
			.timeout(Duration.ofSeconds(30))
			.build();

		ExecResult execResult = dockerSandbox.exec(spec);

		// Step 3: Verify result (simulated parsing)
		assertThat(execResult.success()).isTrue();
		assertThat(execResult.mergedLog()).contains("\"status\": \"success\"");
		assertThat(execResult.mergedLog()).contains("SWE Agent execution");

		// ASSERT: Complete pattern works end-to-end with real Docker execution
		assertThat(execResult.exitCode()).isEqualTo(0);
		assertThat(execResult.duration()).isPositive();
		assertThat(execResult.hasOutput()).isTrue();
	}

	@Test
	void testPythonScriptExecution() throws Exception {
		// CRITICAL TEST: Prove Python script execution works (important for SWE agent)

		// Arrange: Test Python script execution
		ExecSpec pythonScript = ExecSpec.builder()
			.command("python3", "-c", "print('Hello from SWE Agent Docker')")
			.timeout(Duration.ofSeconds(30))
			.build();

		// Act
		ExecResult result = dockerSandbox.exec(pythonScript);

		// Assert: Python script execution works
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).contains("Hello from SWE Agent Docker");
	}

}