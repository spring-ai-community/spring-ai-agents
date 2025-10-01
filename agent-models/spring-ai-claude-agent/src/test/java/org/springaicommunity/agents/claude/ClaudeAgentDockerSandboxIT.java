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

package org.springaicommunity.agents.claude;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springaicommunity.agents.claude.sdk.ClaudeAgentClient;
import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.model.AbstractAgentModelTCK;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.sandbox.DockerSandbox;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TCK test implementation for ClaudeAgentModel with DockerSandbox.
 *
 * <p>
 * Tests the ClaudeAgentModel implementation against the standard agent model TCK test
 * suite using DockerSandbox for command execution. These tests verify that Claude Code
 * CLI integration works correctly with Docker-based isolation.
 * </p>
 *
 * <p>
 * Run with: mvn test -Dtest=ClaudeCodeDockerSandboxIT -Dsandbox.integration.test=true
 * </p>
 *
 * <p>
 * Requirements:
 * </p>
 * <ul>
 * <li>Docker to be available</li>
 * <li>Access to ghcr.io/spring-ai-community/agents-runtime:latest image</li>
 * <li>Environment variable ANTHROPIC_API_KEY set with valid API key</li>
 * <li>Valid Claude API credentials configured</li>
 * </ul>
 *
 * @author Mark Pollack
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@EnabledIfSystemProperty(named = "sandbox.integration.test", matches = "true")
class ClaudeCodeDockerSandboxIT extends AbstractAgentModelTCK {

	@BeforeEach
	void setUp() {
		try {
			// Create DockerSandbox with agents-runtime image
			this.sandbox = new DockerSandbox("ghcr.io/spring-ai-community/agents-runtime:latest", List.of());

			// Create Claude Code CLI with Docker working directory
			ClaudeAgentClient claudeApi = ClaudeAgentClient.create(CLIOptions.defaultOptions(), sandbox.workDir());

			// Create agent options
			ClaudeAgentOptions options = ClaudeAgentOptions.builder()
				.model("claude-sonnet-4-20250514")
				.timeout(Duration.ofMinutes(3))
				.yolo(true) // Enable YOLO mode for autonomous operation
				.build();

			// Create agent model
			this.agentModel = new ClaudeAgentModel(claudeApi, options, sandbox);

			// Verify Claude CLI is available before running tests
			assumeTrue(agentModel.isAvailable(), "Claude CLI must be available for integration tests");
		}
		catch (ClaudeSDKException e) {
			assumeTrue(false, "Failed to initialize Claude CLI: " + e.getMessage());
		}
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return ClaudeAgentOptions.builder()
			.model("claude-sonnet-4-20250514")
			.timeout(Duration.ofSeconds(10)) // Short timeout for timeout testing
			.yolo(true)
			.build();
	}

}