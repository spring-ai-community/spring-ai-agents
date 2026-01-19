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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.tck.AbstractAgentModelTCK;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.sandbox.docker.DockerSandbox;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TCK test implementation for GeminiAgentModel with DockerSandbox.
 *
 * <p>
 * Tests the GeminiAgentModel implementation against the standard agent model TCK test
 * suite using DockerSandbox for command execution. These tests verify that Gemini CLI
 * integration works correctly with Docker-based isolation.
 * </p>
 *
 * <p>
 * Run with: mvn test -Dtest=GeminiAgentModelDockerSandboxIT
 * -Dsandbox.integration.test=true
 * </p>
 *
 * <p>
 * Requirements:
 * </p>
 * <ul>
 * <li>Docker to be available</li>
 * <li>Access to ghcr.io/spring-ai-community/agents-runtime:latest image</li>
 * <li>Environment variable GEMINI_API_KEY or GOOGLE_API_KEY set with valid API key</li>
 * <li>Valid Google AI Studio API credentials configured</li>
 * </ul>
 *
 * @author Mark Pollack
 */
@EnabledIf("hasGeminiApiKey")
@EnabledIfSystemProperty(named = "sandbox.integration.test", matches = "true")
class GeminiAgentModelDockerSandboxIT extends AbstractAgentModelTCK {

	/**
	 * Check if either GEMINI_API_KEY or GOOGLE_API_KEY is available.
	 */
	static boolean hasGeminiApiKey() {
		String geminiKey = System.getenv("GEMINI_API_KEY");
		String googleKey = System.getenv("GOOGLE_API_KEY");
		return (geminiKey != null && !geminiKey.trim().isEmpty()) || (googleKey != null && !googleKey.trim().isEmpty());
	}

	@BeforeEach
	void setUp() {
		try {
			// Create DockerSandbox with agents-runtime image
			this.sandbox = new DockerSandbox("ghcr.io/spring-ai-community/agents-runtime:latest", List.of());

			// Create Gemini CLI with debug options and Docker working directory
			CLIOptions cliOptions = CLIOptions.builder().debug(true).yoloMode(true).build();
			GeminiClient geminiApi = GeminiClient.create(cliOptions, sandbox.workDir());

			// Create agent options
			GeminiAgentOptions options = GeminiAgentOptions.builder()
				.model("gemini-2.0-flash-exp")
				.timeout(Duration.ofMinutes(3))
				.yolo(true) // Enable yolo mode for autonomous operation
				.build();

			// Create agent model
			this.agentModel = new GeminiAgentModel(geminiApi, options, sandbox);

			// Verify Gemini CLI is available before running tests
			assumeTrue(agentModel.isAvailable(), "Gemini CLI must be available for integration tests");
		}
		catch (GeminiSDKException e) {
			assumeTrue(false, "Failed to initialize Gemini CLI: " + e.getMessage());
		}
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return GeminiAgentOptions.builder()
			.model("gemini-2.0-flash-exp")
			.timeout(Duration.ofSeconds(10)) // Short timeout for timeout testing
			.yolo(true)
			.build();
	}

}