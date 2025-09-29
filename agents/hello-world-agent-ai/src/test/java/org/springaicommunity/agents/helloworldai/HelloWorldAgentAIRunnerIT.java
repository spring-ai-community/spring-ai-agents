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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for HelloWorldAgentAI via JBang agents.java launcher. Tests the
 * end-to-end JBang runner functionality with the hello-world-agent-ai that uses
 * AgentClient to invoke real AI agents.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class HelloWorldAgentAIRunnerIT {

	@TempDir
	Path tempDir;

	/**
	 * Get the launcher.java file path in the jbang directory.
	 */
	private Path getLauncherJavaFile() {
		// Find project root by walking up directories looking for jbang/launcher.java
		Path current = Path.of(System.getProperty("user.dir"));
		for (int i = 0; i < 5; i++) {
			Path launcherJava = current.resolve("jbang/launcher.java");
			if (Files.exists(launcherJava)) {
				// Found project root with jbang/launcher.java
				return launcherJava;
			}
			current = current.getParent();
			if (current == null) {
				break;
			}
		}
		throw new RuntimeException("Could not find jbang/launcher.java in project root");
	}

	/**
	 * Get the JBang executable path.
	 */
	private String getJBangExecutable() {
		String jbangHome = System.getenv("JBANG_HOME");
		System.out.println("jbang home " + jbangHome);
		if (jbangHome != null) {
			return jbangHome + "/bin/jbang";
		}
		// Fallback to PATH lookup
		return "jbang";
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
	void testJBangAgentsLauncherWithClaudeAI() throws Exception {
		// Arrange
		String fileName = "ai-greeting.txt";
		String contentDescription = "a creative greeting message about AI agents";

		// Get the launcher.java file in jbang directory
		Path launcherJavaPath = getLauncherJavaFile();

		// Act - Execute JBang launcher.java with hello-world-agent-ai using Claude
		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world-agent-ai", "path=" + fileName,
					"content=" + contentDescription, "provider=claude")
			.directory(tempDir.toFile())
			.timeout(120, TimeUnit.SECONDS) // AI agents need more time
			.readOutput(true)
			.execute();

		// Debug output if test fails
		if (result.getExitValue() != 0) {
			System.out.println("JBang output:");
			System.out.println(result.outputUTF8());
		}

		// Assert
		assertEquals(0, result.getExitValue(),
				"JBang process should exit successfully. Output: " + result.outputUTF8());

		// Verify that the AI agent created the specified file
		Path createdFile = tempDir.resolve(fileName);
		System.out.println("Expected file path: " + createdFile.toAbsolutePath());
		if (Files.exists(createdFile)) {
			String content = Files.readString(createdFile);
			assertFalse(content.isBlank(), "AI-generated content should not be blank");
			System.out.println("AI generated content: " + content);
		}

		// The output should indicate AI agent completion
		String output = result.outputUTF8();
		assertTrue(output.contains("AI agent completed"), "Output should indicate AI agent completion: " + output);
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
	void testJBangAgentsLauncherWithGeminiAI() throws Exception {
		// Arrange
		String fileName = "gemini-info.txt";
		String contentDescription = "information about Gemini AI capabilities";

		// Get the launcher.java file in jbang directory
		Path launcherJavaPath = getLauncherJavaFile();

		// Act - Execute JBang launcher.java with hello-world-agent-ai using Gemini
		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world-agent-ai", "path=" + fileName,
					"content=" + contentDescription, "provider=gemini")
			.directory(tempDir.toFile())
			.timeout(120, TimeUnit.SECONDS) // AI agents need more time
			.readOutput(true)
			.execute();

		// Debug output if test fails
		if (result.getExitValue() != 0) {
			System.out.println("JBang output:");
			System.out.println(result.outputUTF8());
		}

		// Assert
		assertEquals(0, result.getExitValue(),
				"JBang process should exit successfully. Output: " + result.outputUTF8());

		// Verify that the AI agent created the specified file
		Path createdFile = tempDir.resolve(fileName);
		System.out.println("Expected file path: " + createdFile.toAbsolutePath());
		if (Files.exists(createdFile)) {
			String content = Files.readString(createdFile);
			assertFalse(content.isBlank(), "AI-generated content should not be blank");
			System.out.println("AI generated content: " + content);
		}

		// The output should indicate AI agent completion
		String output = result.outputUTF8();
		assertTrue(output.contains("AI agent completed"), "Output should indicate AI agent completion: " + output);
	}

	@Test
	void testJBangAgentsLauncherWithDefaultSettings() throws Exception {
		// Get the launcher.java file in jbang directory
		Path launcherJavaPath = getLauncherJavaFile();

		// Act - Execute JBang launcher.java with hello-world-agent-ai using defaults
		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world-agent-ai")
			.directory(tempDir.toFile())
			.timeout(60, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		// Debug output
		System.out.println("JBang output:");
		System.out.println(result.outputUTF8());

		// Assert - Should run but may fail due to missing API keys/CLI tools
		// We just verify the agent is recognized and attempts to execute
		String output = result.outputUTF8();
		assertFalse(output.contains("No executor found for agent"), "Agent should be found and registered: " + output);
	}

	@Test
	void testJBangAgentsLauncherWithInvalidProvider() throws Exception {
		// Get the launcher.java file in jbang directory
		Path launcherJavaPath = getLauncherJavaFile();

		// Act - Execute JBang launcher.java with invalid provider
		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world-agent-ai",
					"provider=invalid-provider")
			.directory(tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		// Assert - Should fail gracefully with meaningful error
		assertNotEquals(0, result.getExitValue(), "JBang process should exit with error code");

		String output = result.outputUTF8();
		assertTrue(output.contains("Failed to create agent model for provider"),
				"Should contain provider error message: " + output);
	}

}