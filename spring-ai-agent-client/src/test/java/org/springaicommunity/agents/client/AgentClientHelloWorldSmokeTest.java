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

package org.springaicommunity.agents.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.model.sandbox.LocalSandbox;
import org.springaicommunity.agents.claude.sdk.ClaudeAgentClient;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.config.ClaudeCliDiscovery;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentOptions;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.util.GeminiCliDiscovery;
import org.springaicommunity.agents.model.AgentModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Smoke test demonstrating AgentClient hello world with two real providers.
 *
 * <p>
 * This is our only high-level end-to-end integration test and is included in the smoke
 * test suite for fast feedback despite taking ~60 seconds to run.
 * </p>
 *
 * <p>
 * This test requires actual CLI tools and API keys to be configured.
 * </p>
 *
 * @author Mark Pollack
 */
class AgentClientHelloWorldSmokeTest {

	private Path tempWorkspace;

	@BeforeEach
	void setUp(@TempDir Path tempDir) throws IOException {
		this.tempWorkspace = tempDir;

		// Create a simple workspace with a text file
		Files.writeString(tempDir.resolve("README.md"), "# Hello World Project\n\nThis is a test project.\n");
	}

	private static final String HELLO_WORLD_GOAL = "Create a simple hello.txt file with the content 'Hello, World!'";

	@Test
	@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
	void testClaudeCodeAgentClient() throws Exception {
		// Skip test if Claude CLI not available
		assumeTrue(isClaudeCliAvailable(), "Claude CLI must be available for this test");

		// Create Claude Code agent
		ClaudeAgentOptions options = ClaudeAgentOptions.builder()
			.model("claude-sonnet-4-20250514")
			.yolo(true) // Bypass permissions for test
			.build();

		ClaudeAgentClient claudeClient = ClaudeAgentClient.create(CLIOptions.defaultOptions(), this.tempWorkspace);
		LocalSandbox sandbox = new LocalSandbox(this.tempWorkspace);
		ClaudeAgentModel agentModel = new ClaudeAgentModel(claudeClient, options, sandbox);

		// Skip if agent not available
		assumeTrue(agentModel.isAvailable(), "Claude agent must be available");

		// Execute hello world test
		AgentClientResponse response = executeHelloWorldTest(agentModel, "Claude");

		System.out.println("Claude response: " + response.getResult());
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
	void testGeminiAgentClient() throws Exception {
		// Skip test if Gemini CLI not available
		assumeTrue(isGeminiCliAvailable(), "Gemini CLI must be available for this test");

		// Create Gemini agent
		GeminiAgentOptions options = GeminiAgentOptions.builder()
			.model("gemini-2.5-flash")
			.yolo(true) // Bypass permissions for test
			.build();

		GeminiClient geminiClient = GeminiClient.create();
		LocalSandbox sandbox = new LocalSandbox(this.tempWorkspace);
		GeminiAgentModel agentModel = new GeminiAgentModel(geminiClient, options, sandbox);

		// Skip if agent not available
		assumeTrue(agentModel.isAvailable(), "Gemini agent must be available");

		// Execute hello world test
		AgentClientResponse response = executeHelloWorldTest(agentModel, "Gemini");

		System.out.println("Gemini response: " + response.getResult());
	}

	/**
	 * Common test execution logic for both providers.
	 */
	private AgentClientResponse executeHelloWorldTest(AgentModel agentModel, String providerName) throws Exception {
		// Create client and execute hello world task
		AgentClient client = AgentClient.create(agentModel);

		AgentClientResponse response = client.goal(HELLO_WORLD_GOAL).workingDirectory(this.tempWorkspace).run();

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotBlank();

		// Verify file was created (if agent was able to create it)
		Path helloFile = this.tempWorkspace.resolve("hello.txt");
		if (Files.exists(helloFile)) {
			String content = Files.readString(helloFile);
			assertThat(content).contains("Hello, World!");
		}

		return response;
	}

	@Test
	void testAgentClientBasicFunctionality(@TempDir Path tempDir) {
		// Test basic client functionality without real agents (mock test)
		MockAgentModel mockModel = new MockAgentModel();

		AgentClient client = AgentClient.create(mockModel);

		AgentClientResponse response = client.goal("Test goal").workingDirectory(tempDir).run();

		assertThat(response).isNotNull();
		assertThat(response.getResult()).isEqualTo("Mock response for: Test goal");
		assertThat(mockModel.lastRequest).isNotNull();
		assertThat(mockModel.lastRequest.goal()).isEqualTo("Test goal");
		assertThat(mockModel.lastRequest.workingDirectory()).isEqualTo(tempDir);
	}

	private boolean isClaudeCliAvailable() {
		try {
			String discoveredPath = ClaudeCliDiscovery.getDiscoveredPath();
			return discoveredPath != null;
		}
		catch (Exception e) {
			return false;
		}
	}

	private boolean isGeminiCliAvailable() {
		try {
			return GeminiCliDiscovery.isGeminiCliAvailable();
		}
		catch (Exception e) {
			return false;
		}
	}

}