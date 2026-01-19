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

package org.springaicommunity.agents.amazonq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springaicommunity.agents.amazonqsdk.AmazonQClient;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.sandbox.LocalSandbox;
import org.springaicommunity.agents.tck.AbstractAgentModelTCK;

import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TCK test implementation for AmazonQAgentModel with LocalSandbox.
 *
 * @author Spring AI Community
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
		disabledReason = "Amazon Q CLI authentication not available in CI environment")
class AmazonQAgentLocalSandboxIT extends AbstractAgentModelTCK {

	@BeforeEach
	void setUp() {
		try {
			// Create LocalSandbox with temp directory
			this.sandbox = new LocalSandbox(tempDir);

			// Create Amazon Q client
			AmazonQClient amazonQClient = AmazonQClient.create(tempDir);

			// Create agent options
			AmazonQAgentOptions options = AmazonQAgentOptions.builder()
				.model("amazon-q-developer")
				.timeout(Duration.ofMinutes(3))
				.trustAllTools(true)
				.build();

			// Create agent model
			this.agentModel = new AmazonQAgentModel(amazonQClient, options, sandbox);

			// Verify Amazon Q CLI is available before running tests
			assumeTrue(agentModel.isAvailable(), "Amazon Q CLI must be available for integration tests");
		}
		catch (Exception e) {
			assumeTrue(false, "Failed to initialize Amazon Q CLI: " + e.getMessage());
		}
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return AmazonQAgentOptions.builder()
			.model("amazon-q-developer")
			.timeout(Duration.ofSeconds(10)) // Short timeout for timeout testing
			.trustAllTools(true)
			.build();
	}

}
