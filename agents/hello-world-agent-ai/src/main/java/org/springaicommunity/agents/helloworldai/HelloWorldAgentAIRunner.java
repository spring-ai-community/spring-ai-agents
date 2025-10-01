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

package org.springaicommunity.agents.helloworldai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.claude.sdk.ClaudeAgentClient;
import org.springaicommunity.agents.claude.sdk.config.ClaudeCliDiscovery;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.core.AgentRunner;
import org.springaicommunity.agents.core.LauncherSpec;
import org.springaicommunity.agents.core.Result;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentOptions;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.util.GeminiCliDiscovery;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.sandbox.LocalSandbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Hello World AI Agent implementation that uses the AgentClient to invoke real AI agents
 * for file creation. This demonstrates the integration between spring-ai-agents
 * components and showcases actual AI-powered code generation.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class HelloWorldAgentAIRunner implements AgentRunner {

	private static final Logger log = LoggerFactory.getLogger(HelloWorldAgentAIRunner.class);

	private static final String DEFAULT_GOAL = "Create a hello.txt file with creative content about the power of AI agents in software development. Make the content inspiring and informative.";

	@Override
	public Result run(LauncherSpec spec) {
		log.info("Executing hello-world-agent-ai");
		try {
			// Parse and validate inputs
			Map<String, Object> inputs = spec.inputs();
			String path = (String) inputs.get("path");
			String content = (String) inputs.get("content");
			String provider = (String) inputs.get("provider");

			// Apply defaults
			if (provider == null || provider.isBlank()) {
				provider = "claude";
			}
			if (content == null || content.isBlank()) {
				content = "Hello World!";
			}

			log.info("Hello-world-agent-ai inputs: path={}, content={}, provider={}", path, content, provider);

			// Validate required inputs
			if (path == null || path.isBlank()) {
				return Result.fail("Missing required input: path");
			}

			// Create AI agent based on provider
			AgentModel agentModel = createAgentModel(provider, spec.cwd());
			if (agentModel == null) {
				return Result.fail("Failed to create agent model for provider: " + provider);
			}

			// Check if agent is available
			if (!agentModel.isAvailable()) {
				return Result.fail("Agent model is not available for provider: " + provider
						+ ". Please check API keys and CLI tools installation.");
			}

			// Construct AI goal from user inputs
			String goal = String.format("Create a file at path '%s' with content: %s", path, content);

			// Create AgentClient and execute the goal
			AgentClient client = AgentClient.create(agentModel);
			log.info("Executing AI agent goal for file: {}", path);

			AgentClientResponse response = client.goal(goal).workingDirectory(spec.cwd()).run();

			// Verify response
			if (response == null) {
				return Result.fail("Agent returned null response");
			}

			log.info("AI agent completed successfully");

			// Check if the requested file was created
			Path targetFile = spec.cwd().resolve(path);
			Map<String, Object> outputs;
			if (Files.exists(targetFile)) {
				String fileContent = Files.readString(targetFile);
				log.info("AI agent successfully created {} with {} characters", path, fileContent.length());
				outputs = Map.of("file_created", true, "path", targetFile.toAbsolutePath().toString(), "content_length",
						fileContent.length(), "ai_response", response.getResult());
			}
			else {
				log.warn("AI agent completed but {} was not found", path);
				outputs = Map.of("file_created", false, "path", targetFile.toAbsolutePath().toString(), "ai_response",
						response.getResult());
			}

			return Result.ok("AI agent completed: " + response.getResult(), outputs);
		}
		catch (Exception e) {
			log.error("Failed to execute AI agent", e);
			return Result.fail("Failed to execute AI agent: " + e.getMessage());
		}
	}

	private AgentModel createAgentModel(String provider, Path workingDirectory) {
		try {
			switch (provider.toLowerCase()) {
				case "claude":
					return createClaudeAgent(workingDirectory);
				case "gemini":
					return createGeminiAgent(workingDirectory);
				default:
					log.error("Unknown provider: {}", provider);
					return null;
			}
		}
		catch (Exception e) {
			log.error("Failed to create agent model for provider: {}", provider, e);
			return null;
		}
	}

	private AgentModel createClaudeAgent(Path workingDirectory) {
		// Check if Claude CLI is available
		if (!isClaudeCliAvailable()) {
			log.warn("Claude CLI not available");
			return null;
		}

		ClaudeAgentOptions options = ClaudeAgentOptions.builder()
			.model("claude-sonnet-4-20250514")
			.yolo(true) // Bypass permissions for automated execution
			.build();

		ClaudeAgentClient claudeClient = ClaudeAgentClient.create(CLIOptions.defaultOptions(), workingDirectory);
		LocalSandbox sandbox = new LocalSandbox(workingDirectory);
		return new ClaudeAgentModel(claudeClient, options, sandbox);
	}

	private AgentModel createGeminiAgent(Path workingDirectory) {
		// Check if Gemini CLI is available
		if (!isGeminiCliAvailable()) {
			log.warn("Gemini CLI not available");
			return null;
		}

		GeminiAgentOptions options = GeminiAgentOptions.builder()
			.model("gemini-2.0-flash-exp")
			.yolo(true) // Bypass permissions for automated execution
			.build();

		GeminiClient geminiClient = GeminiClient.create();
		LocalSandbox sandbox = new LocalSandbox(workingDirectory);
		return new GeminiAgentModel(geminiClient, options, sandbox);
	}

	private boolean isClaudeCliAvailable() {
		return ClaudeCliDiscovery.isClaudeCliAvailable();
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