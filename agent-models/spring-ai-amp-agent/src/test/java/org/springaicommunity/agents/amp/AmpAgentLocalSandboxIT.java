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

package org.springaicommunity.agents.amp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springaicommunity.agents.ampsdk.AmpClient;
import org.springaicommunity.agents.ampsdk.types.ExecuteOptions;
import org.springaicommunity.agents.tck.AbstractAgentModelTCK;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.sandbox.LocalSandbox;

import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TCK test implementation for AmpAgentModel with LocalSandbox.
 *
 * <p>
 * Tests the AmpAgentModel implementation against the standard agent model TCK test suite
 * using LocalSandbox for command execution. These tests verify that Amp CLI integration
 * works correctly with local execution.
 * </p>
 *
 * <p>
 * Requirements:
 * </p>
 * <ul>
 * <li>Amp CLI to be installed and discoverable</li>
 * <li>Amp authentication configured (via `amp login` or AMP_API_KEY)</li>
 * </ul>
 *
 * @author Spring AI Community
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
		disabledReason = "Amp CLI not available in CI environment")
class AmpAgentLocalSandboxIT extends AbstractAgentModelTCK {

	@BeforeEach
	void setUp() {
		try {
			// Create LocalSandbox with temp directory
			this.sandbox = new LocalSandbox(tempDir);

			// Create Amp client with default options
			ExecuteOptions executeOptions = ExecuteOptions.builder()
				.dangerouslyAllowAll(true)
				.timeout(Duration.ofMinutes(3))
				.build();

			AmpClient ampClient = AmpClient.create(executeOptions, tempDir);

			// Create agent options
			AmpAgentOptions options = AmpAgentOptions.builder()
				.model("amp-default")
				.timeout(Duration.ofMinutes(3))
				.dangerouslyAllowAll(true)
				.build();

			// Create agent model
			this.agentModel = new AmpAgentModel(ampClient, options, sandbox);

			// Verify Amp CLI is available before running tests
			assumeTrue(agentModel.isAvailable(), "Amp CLI must be available for integration tests");
		}
		catch (Exception e) {
			assumeTrue(false, "Failed to initialize Amp CLI: " + e.getMessage());
		}
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return AmpAgentOptions.builder()
			.model("amp-default")
			.timeout(Duration.ofSeconds(10)) // Short timeout for timeout testing
			.dangerouslyAllowAll(true)
			.build();
	}

}
