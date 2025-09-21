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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi;
import org.springaicommunity.agents.model.AbstractAgentModelTCK;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.sandbox.DockerSandbox;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TCK test implementation for SweAgentModel with DockerSandbox.
 *
 * <p>
 * Tests the SweAgentModel implementation against the standard agent model TCK test suite
 * using DockerSandbox for command execution. These tests verify that SWE Agent CLI
 * integration works correctly with Docker-based isolation using the new Sandbox pattern.
 * </p>
 *
 * <p>
 * Run with: mvn test -Dtest=SweAgentDockerSandboxIT -Dsandbox.integration.test=true
 * </p>
 *
 * <p>
 * Requirements:
 * </p>
 * <ul>
 * <li>Docker to be available</li>
 * <li>Access to ghcr.io/spring-ai-community/agents-runtime:latest image</li>
 * <li>Environment variable OPENAI_API_KEY set with valid API key</li>
 * <li>Valid OpenAI API credentials configured</li>
 * </ul>
 *
 * @author Mark Pollack
 */
@DisabledIfSystemProperty(named = "skipIntegrationTests", matches = "true")
@EnabledIfSystemProperty(named = "sandbox.integration.test", matches = "true")
class SweAgentDockerSandboxIT extends AbstractAgentModelTCK {

	@BeforeEach
	void setUp() {
		// Create DockerSandbox with agents-runtime image
		this.sandbox = new DockerSandbox("ghcr.io/spring-ai-community/agents-runtime:latest", List.of());

		// Get SWE CLI executable path (should be available in Docker image)
		String executablePath = "mini"; // Should be in PATH within Docker container

		// Create SWE CLI API
		SweCliApi sweCliApi = new SweCliApi(executablePath);

		// Create agent options
		SweAgentOptions options = SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofMinutes(3))
			.maxIterations(5) // Conservative iteration limit for tests
			.verbose(true)
			.executablePath(executablePath)
			.build();

		// Create agent model with new Sandbox pattern
		this.agentModel = new SweAgentModel(sweCliApi, options, sandbox);

		// Note: isAvailable() may not work correctly in Docker context initially
		// The TCK tests will determine if the integration is working
		assumeTrue(agentModel != null, "SWE Agent model should be created successfully");
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return SweAgentOptions.builder()
			.model("gpt-4o-mini")
			.timeout(Duration.ofSeconds(10)) // Short timeout for timeout testing
			.maxIterations(2) // Very low iterations for timeout test
			.verbose(true)
			.build();
	}

}