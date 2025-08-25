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

package org.springaicommunity.agents.claudecode.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * MVP prototype for validating zt-exec integration with Claude CLI. This class
 * demonstrates basic process execution, streaming output, and error handling.
 */
public class ClaudeCliPrototype {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeCliPrototype.class);

	private final Path workingDirectory;

	private final String claudeCommand;

	public ClaudeCliPrototype(Path workingDirectory) {
		this(workingDirectory, null);
	}

	public ClaudeCliPrototype(Path workingDirectory, String claudeCliPath) {
		this.workingDirectory = workingDirectory;
		this.claudeCommand = claudeCliPath != null ? claudeCliPath : findClaudeCommand();
	}

	/**
	 * Test basic ProcessExecutor with Claude CLI
	 */
	public void testBasicExecution() throws Exception {
		logger.info("Testing basic Claude CLI execution");

		ProcessResult result = new ProcessExecutor().command(claudeCommand, "--version")
			.directory(workingDirectory.toFile())
			.timeout(10, TimeUnit.SECONDS)
			.redirectError(Slf4jStream.of(getClass()).asError())
			.readOutput(true)
			.execute();

		logger.info("Claude CLI version: {}", result.outputUTF8().trim());
		logger.info("Exit code: {}", result.getExitValue());
	}

	/**
	 * Test streaming JSON output from Claude CLI
	 */
	public void testStreamingOutput(String prompt) throws Exception {
		logger.info("Testing streaming output with prompt: {}", prompt);

		LogOutputStream lineProcessor = new LogOutputStream() {
			@Override
			protected void processLine(String line) {
				logger.info("Received line: {}", line);
				// In real implementation, this would parse JSON and emit messages
			}
		};

		ProcessResult result = new ProcessExecutor()
			.command(claudeCommand, "--output-format", "stream-json", "--verbose", "--print", prompt)
			.directory(workingDirectory.toFile())
			.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java")
			.timeout(60, TimeUnit.SECONDS)
			.redirectOutput(lineProcessor)
			.redirectError(Slf4jStream.of(getClass()).asError())
			.readOutput(true)
			.execute();

		logger.info("Streaming completed with exit code: {}", result.getExitValue());
	}

	/**
	 * Test line-by-line processing for real-time streaming
	 */
	public void testRealTimeStreaming(String prompt) throws Exception {
		logger.info("Testing real-time streaming");

		StringBuilder jsonBuffer = new StringBuilder();

		LogOutputStream realtimeProcessor = new LogOutputStream() {
			@Override
			protected void processLine(String line) {
				logger.debug("Processing line: {}", line);

				// Simple JSON detection - in real implementation would be more robust
				if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
					logger.info("Complete JSON message: {}", line);
					// Here we would parse and emit the message
				}
				else {
					// Buffer partial JSON
					jsonBuffer.append(line).append("\n");
				}
			}
		};

		ProcessResult result = new ProcessExecutor()
			.command(claudeCommand, "--output-format", "stream-json", "--verbose", "--print", prompt)
			.directory(workingDirectory.toFile())
			.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java")
			.timeout(60, TimeUnit.SECONDS)
			.redirectOutput(realtimeProcessor)
			.redirectError(Slf4jStream.of(getClass()).asError())
			.readOutput(true)
			.execute();

		if (jsonBuffer.length() > 0) {
			logger.warn("Remaining buffer content: {}", jsonBuffer.toString());
		}

		logger.info("Real-time streaming completed with exit code: {}", result.getExitValue());
	}

	/**
	 * Discover Claude CLI command path
	 */
	private String findClaudeCommand() {
		// Check common locations for Claude CLI
		String[] candidates = { "claude", "claude-code", "~/.local/bin/claude" };

		for (String candidate : candidates) {
			try {
				ProcessResult result = new ProcessExecutor().command(candidate, "--version")
					.timeout(5, TimeUnit.SECONDS)
					.readOutput(true)
					.execute();

				if (result.getExitValue() == 0) {
					logger.info("Found Claude CLI at: {}", candidate);
					return candidate;
				}
			}
			catch (Exception e) {
				logger.debug("Claude CLI not found at: {}", candidate);
			}
		}

		logger.warn("Claude CLI not found, using default 'claude'");
		return "claude";
	}

	/**
	 * Main method for testing the prototype
	 */
	public static void main(String[] args) {
		try {
			Path workingDir = Path.of(System.getProperty("user.dir"));
			ClaudeCliPrototype prototype = new ClaudeCliPrototype(workingDir);

			// Test basic execution
			prototype.testBasicExecution();

			// Test streaming with a simple prompt
			prototype.testStreamingOutput("What is 2+2?");

			// Test real-time streaming
			prototype.testRealTimeStreaming("Explain the concept of recursion briefly");

		}
		catch (Exception e) {
			logger.error("Prototype test failed", e);
			System.exit(1);
		}
	}

}