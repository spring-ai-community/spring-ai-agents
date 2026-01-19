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
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi;
import org.springaicommunity.agents.tck.AbstractAgentModelTCK;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.sandbox.LocalSandbox;

import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TCK test implementation for SweAgentModel with LocalSandbox.
 *
 * <p>
 * Tests the SweAgentModel implementation against the standard agent model TCK test suite
 * using LocalSandbox for command execution. These tests verify that SWE Agent CLI
 * integration works correctly with local execution using the new Sandbox pattern.
 * </p>
 *
 * <p>
 * Requirements:
 * </p>
 * <ul>
 * <li>SWE Agent CLI (mini) to be installed and discoverable</li>
 * <li>Environment variable OPENAI_API_KEY set with valid API key</li>
 * <li>Valid OpenAI API credentials configured</li>
 * </ul>
 *
 * @author Mark Pollack
 */
@DisabledIfSystemProperty(named = "skipIntegrationTests", matches = "true")
class SweAgentLocalSandboxIT extends AbstractAgentModelTCK {

	@BeforeEach
	void setUp() {
		// Create LocalSandbox with temp directory
		this.sandbox = new LocalSandbox(tempDir);

		// Get SWE CLI executable path
		String executablePath = System.getProperty("swe.cli.path");
		if (executablePath == null) {
			executablePath = System.getenv("SWE_CLI_PATH");
		}
		if (executablePath == null) {
			executablePath = "/home/mark/.local/bin/mini"; // Hardcoded fallback path
		}

		// Create SWE CLI API
		SweCliApi sweCliApi = new SweCliApi(executablePath);
		assumeTrue(sweCliApi.isAvailable(), "SWE Agent CLI not available at: " + executablePath);

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

		// Verify SWE Agent CLI is available before running tests
		assumeTrue(agentModel.isAvailable(), "SWE Agent CLI must be available for integration tests");
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