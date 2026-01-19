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

package org.springaicommunity.agents.codex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springaicommunity.agents.codexsdk.CodexClient;
import org.springaicommunity.agents.codexsdk.types.ExecuteOptions;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.sandbox.LocalSandbox;
import org.springaicommunity.agents.tck.AbstractAgentModelTCK;
import org.zeroturnaround.exec.ProcessExecutor;

import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TCK test implementation for CodexAgentModel with LocalSandbox.
 *
 * @author Spring AI Community
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
		disabledReason = "Codex CLI not available in CI environment")
class CodexAgentLocalSandboxIT extends AbstractAgentModelTCK {

	@BeforeEach
	void setUp() {
		try {
			// Initialize git repository (Codex requires this)
			new ProcessExecutor().command("git", "init").directory(tempDir.toFile()).execute();

			// Create LocalSandbox with temp directory
			this.sandbox = new LocalSandbox(tempDir);

			// Create Codex client with default options
			ExecuteOptions executeOptions = ExecuteOptions.builder()
				.fullAuto(true)
				.timeout(Duration.ofMinutes(3))
				.skipGitCheck(false)
				.build();

			CodexClient codexClient = CodexClient.create(executeOptions, tempDir);

			// Create agent options
			CodexAgentOptions options = CodexAgentOptions.builder()
				.model("gpt-5-codex")
				.timeout(Duration.ofMinutes(3))
				.fullAuto(true)
				.skipGitCheck(false)
				.build();

			// Create agent model
			this.agentModel = new CodexAgentModel(codexClient, options, sandbox);

			// Verify Codex CLI is available before running tests
			assumeTrue(agentModel.isAvailable(), "Codex CLI must be available for integration tests");
		}
		catch (Exception e) {
			assumeTrue(false, "Failed to initialize Codex CLI: " + e.getMessage());
		}
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return CodexAgentOptions.builder()
			.model("gpt-5-codex")
			.timeout(Duration.ofSeconds(10)) // Short timeout for timeout testing
			.fullAuto(true)
			.skipGitCheck(false)
			.build();
	}

}
