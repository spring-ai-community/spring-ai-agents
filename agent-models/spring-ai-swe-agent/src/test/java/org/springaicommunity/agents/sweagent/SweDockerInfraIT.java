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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springaicommunity.agents.tck.sandbox.AbstractDockerInfrastructureTCK;
import org.springaicommunity.sandbox.ExecResult;
import org.springaicommunity.sandbox.ExecSpec;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker infrastructure tests for SWE agent runtime environment.
 *
 * <p>
 * These tests prove that the DockerSandbox infrastructure works correctly with
 * SWE-agent-specific requirements including Python execution and environment variables.
 * These tests focus on sandbox infrastructure rather than actual SWE agent CLI
 * functionality.
 * </p>
 *
 * <p>
 * Run with: mvn test -Dtest=SweDockerInfraIT -Dsandbox.integration.test=true
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
class SweDockerInfraIT extends AbstractDockerInfrastructureTCK {

	@Override
	protected String getDockerImage() {
		return "ghcr.io/spring-ai-community/agents-runtime:latest";
	}

	@Override
	protected Map<String, String> getAgentSpecificEnvironment() {
		return Map.of("OPENAI_API_KEY", "test-key", "SWE_AGENT_CONFIG", "test-config");
	}

	@Override
	protected String getExpectedAgentOutput() {
		return "SWE Agent execution success";
	}

	/**
	 * Test Python runtime availability for SWE Agent.
	 */
	@Test
	void testSweSpecificPythonRuntime() throws Exception {
		// CRITICAL TEST: Prove Python execution works in Docker (required for SWE Agent)

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

	/**
	 * Test SWE Agent-specific environment variables.
	 */
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

}