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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springaicommunity.agents.tck.sandbox.AbstractDockerInfrastructureTCK;
import org.springaicommunity.sandbox.ExecResult;
import org.springaicommunity.sandbox.ExecSpec;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker infrastructure tests for Gemini agent runtime environment.
 *
 * <p>
 * These tests prove that the DockerSandbox infrastructure works correctly with
 * Gemini-specific requirements including Node.js execution and environment variables.
 * These tests focus on sandbox infrastructure rather than actual Gemini CLI
 * functionality.
 * </p>
 *
 * <p>
 * Run with: mvn test -Dtest=GeminiDockerInfraIT -Dsandbox.integration.test=true
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
class GeminiDockerInfraIT extends AbstractDockerInfrastructureTCK {

	@Override
	protected String getDockerImage() {
		return "ghcr.io/spring-ai-community/agents-runtime:latest";
	}

	@Override
	protected Map<String, String> getAgentSpecificEnvironment() {
		return Map.of("GEMINI_API_KEY", "test-key", "GOOGLE_API_KEY", "test-google-key");
	}

	@Override
	protected String getExpectedAgentOutput() {
		return "Gemini execution success";
	}

	/**
	 * Test Node.js runtime availability for Gemini CLI.
	 */
	@Test
	void testGeminiSpecificNodeJsRuntime() throws Exception {
		// CRITICAL TEST: Prove Node.js execution works in Docker (required for Gemini
		// CLI)

		// Arrange: Test Node.js is available
		ExecSpec nodeTest = ExecSpec.builder().command("node", "--version").timeout(Duration.ofSeconds(30)).build();

		// Act
		ExecResult result = dockerSandbox.exec(nodeTest);

		// Assert: Node.js is available in the agents-runtime image
		assertThat(result.success()).isTrue();
		assertThat(result.mergedLog()).startsWith("v");
	}

	/**
	 * Test Gemini-specific environment variables.
	 */
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

}