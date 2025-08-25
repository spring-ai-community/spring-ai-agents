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

import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for GeminiAgentModel with real Gemini CLI.
 *
 * <p>
 * These tests require:
 * </p>
 * <ul>
 * <li>Gemini CLI to be installed and available in PATH</li>
 * <li>Environment variable GEMINI_API_KEY or GOOGLE_API_KEY set with valid API key</li>
 * <li>Note: Gemini CLI prefers GOOGLE_API_KEY over GEMINI_API_KEY if both are set</li>
 * <li>Valid Google AI Studio API credentials configured</li>
 * </ul>
 *
 * @author Mark Pollack
 */
@EnabledIf("hasGeminiApiKey")
class GeminiAgentModelIT {

	private GeminiAgentModel agentModel;

	private GeminiAgentOptions options;

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
		// Create agent options that will be used by all tests
		String executablePath = System.getenv("GEMINI_CLI_PATH");

		if (executablePath == null) {
			// Use the hardcoded nvm path like Claude does
			executablePath = "/home/mark/.nvm/versions/node/v22.15.0/bin/gemini";
			System.out.println("Using hardcoded nvm path: " + executablePath);
		}
		else {
			System.out.println("Using GEMINI_CLI_PATH from environment: " + executablePath);
		}

		options = GeminiAgentOptions.builder()
			.model("gemini-2.0-flash-exp")
			.timeout(Duration.ofMinutes(3))
			.executablePath(executablePath)
			.build();
	}

	private GeminiAgentModel createAgentModel(Path workingDirectory) {
		try {
			// Set the executable path system property BEFORE creating GeminiClient
			if (options.getExecutablePath() != null) {
				System.setProperty("gemini.cli.path", options.getExecutablePath());
				System.out.println("Set gemini.cli.path to: " + options.getExecutablePath());
			}

			// Create Gemini CLI with the specific working directory
			GeminiClient geminiApi = GeminiClient.create(CLIOptions.defaultOptions(), workingDirectory);

			GeminiAgentModel model = new GeminiAgentModel(geminiApi, options);

			// Verify Gemini CLI is available before running tests
			assumeTrue(model.isAvailable(), "Gemini CLI must be available for integration tests");

			return model;
		}
		catch (GeminiSDKException e) {
			assumeTrue(false, "Failed to initialize Gemini CLI: " + e.getMessage());
			return null; // Never reached
		}
	}

	@Test
	void testCodeFixTask(@TempDir Path tempDir) throws IOException {
		// Create agent model with the temp directory as working directory
		GeminiAgentModel agentModel = createAgentModel(tempDir);

		// Create a broken Java file
		Path brokenFile = tempDir.resolve("Broken.java");
		String brokenCode = """
				public class Broken {
				    public void test() {
				        System.out.println("Hello World"  // Missing closing parenthesis
				    }
				}
				""";
		Files.writeString(brokenFile, brokenCode);

		// Verify file doesn't compile initially
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int initialCompilation = compiler.run(null, null, null, brokenFile.toString());
		assertThat(initialCompilation).isNotEqualTo(0); // Should fail to compile

		// Create agent request to fix the syntax error
		AgentTaskRequest request = AgentTaskRequest.builder("Fix the syntax error in Broken.java", tempDir)
			.options(options)
			.build();

		// Execute the agent
		AgentResponse result = agentModel.call(request);

		// Debug: Print agent messages to understand what happened
		System.out.println("Agent Status: " + result.getResult().getMetadata().getFinishReason());
		System.out.println("Agent Output: " + result.getResult().getOutput());

		// Assert the call was successful
		assertThat(result.getResult().getMetadata().getFinishReason()).isIn("SUCCESS", "SUCCESS");
		assertThat(result.getMetadata().getDuration()).isNotNull();
		assertThat(result.getMetadata().getDuration().toSeconds()).isGreaterThan(0);

		// Verify the file now compiles
		String fixedContent = Files.readString(brokenFile);

		// Check if the agent actually modified the file
		if (fixedContent.equals(brokenCode)) {
			fail("File was not modified by the agent. Original content remains unchanged.");
		}

		// The fixed content should have the closing parenthesis
		assertThat(fixedContent).as("File should have been fixed with closing parenthesis")
			.contains("System.out.println(\"Hello World\");");

		int finalCompilation = compiler.run(null, null, null, brokenFile.toString());
		assertThat(finalCompilation).as("Fixed file should compile successfully").isEqualTo(0);
	}

	@Test
	void testSimpleFileCreation(@TempDir Path tempDir) throws IOException {
		// Create agent model with the temp directory as working directory
		GeminiAgentModel agentModel = createAgentModel(tempDir);

		// Create agent request to create a simple file
		AgentTaskRequest request = AgentTaskRequest
			.builder("Create a file named greeting.txt with the content 'Hello, Spring AI!'", tempDir)
			.options(options)
			.build();

		// Execute the agent
		AgentResponse result = agentModel.call(request);

		// Debug: Print agent messages to understand what happened
		System.out.println("Agent Status: " + result.getResult().getMetadata().getFinishReason());
		System.out.println("Agent Output: " + result.getResult().getOutput());

		// Assert the call was successful
		assertThat(result.getResult().getMetadata().getFinishReason()).isIn("SUCCESS", "SUCCESS");

		// Verify the file was created
		Path greetingFile = tempDir.resolve("greeting.txt");
		assertThat(Files.exists(greetingFile)).as("File greeting.txt should have been created").isTrue();

		// Verify the content if file exists
		if (Files.exists(greetingFile)) {
			String content = Files.readString(greetingFile);
			assertThat(content).contains("Hello", "Spring AI");
		}
	}

	@Test
	void testFileListAccess(@TempDir Path tempDir) throws IOException {
		// Create agent model with the temp directory as working directory
		GeminiAgentModel agentModel = createAgentModel(tempDir);

		// Create files that should and shouldn't be accessible
		Files.writeString(tempDir.resolve("allowed.java"), "public class Allowed {}");
		Files.writeString(tempDir.resolve("restricted.txt"), "This should not be accessible");
		Files.createDirectories(tempDir.resolve(".git"));
		Files.writeString(tempDir.resolve(".git/config"), "[core] repositoryformatversion = 0");

		// Create agent request with restrictive globs
		AgentTaskRequest request = AgentTaskRequest.builder("List all accessible files and their contents", tempDir)
			.options(options)
			.build();

		// Execute the agent
		AgentResponse result = agentModel.call(request);

		// Agent should succeed and see all files in the directory
		assertThat(result.getResult().getMetadata().getFinishReason()).isIn("SUCCESS", "SUCCESS");

		// The response should mention the Java file but not the restricted files
		String response = result.getResult().getOutput();
		assertThat(response.toLowerCase()).contains("allowed.java");
		// Note: Can't guarantee agent won't mention restricted files in response,
		// but it shouldn't be able to modify them
	}

	@Test
	void testTimeoutHandling(@TempDir Path tempDir) throws IOException {
		// Create agent model with the temp directory as working directory
		GeminiAgentModel agentModel = createAgentModel(tempDir);

		// Create a task that might take a while
		Files.writeString(tempDir.resolve("complex.java"), "public class Complex { /* TODO: implement */ }");

		// Create agent with very short timeout
		GeminiAgentOptions shortTimeoutOptions = GeminiAgentOptions.builder()
			.model("gemini-pro")
			.timeout(Duration.ofSeconds(1)) // Very short timeout
			.build();

		AgentTaskRequest request = AgentTaskRequest
			.builder("Write a comprehensive implementation of a complex algorithm", tempDir)
			.options(shortTimeoutOptions)
			.build();

		// Execute the agent - should complete quickly or timeout gracefully
		AgentResponse result = agentModel.call(request);

		// Should not crash and should return a reasonable result
		assertThat(result.getResult().getMetadata().getFinishReason()).isIn("SUCCESS", "SUCCESS", "ERROR");
		assertThat(result.getMetadata().getDuration()).isNotNull();
	}

	@Test
	void testAgentAvailability(@TempDir Path tempDir) {
		// Create agent model with a temp directory
		GeminiAgentModel agentModel = createAgentModel(tempDir);

		// Test that isAvailable works correctly
		assertThat(agentModel.isAvailable()).isTrue();
	}

}