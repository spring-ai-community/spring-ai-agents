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

package org.springaicommunity.agents.amazonqsdk.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.amazonqsdk.exceptions.AmazonQSDKException;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteOptions;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteResult;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles communication with the Amazon Q CLI process.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class CLITransport {

	private static final Logger logger = LoggerFactory.getLogger(CLITransport.class);

	private final String qCliPath;

	private final Path workingDirectory;

	public CLITransport(String qCliPath, Path workingDirectory) {
		this.qCliPath = qCliPath;
		this.workingDirectory = workingDirectory;
	}

	/**
	 * Executes a prompt via Amazon Q CLI.
	 * @param prompt the prompt to execute
	 * @param options execution options
	 * @return the execution result
	 * @throws AmazonQSDKException if execution fails
	 */
	public ExecuteResult execute(String prompt, ExecuteOptions options) {
		logger.info("Executing Amazon Q with prompt: {}", prompt);
		Instant startTime = Instant.now();

		try {
			List<String> command = buildCommand(prompt, options);
			logger.debug("Running command: {}", String.join(" ", command));

			long timeoutMillis = options.getTimeout().toMillis();

			ProcessResult result = new ProcessExecutor().command(command)
				.directory(workingDirectory.toFile())
				.timeout(timeoutMillis, TimeUnit.MILLISECONDS)
				.readOutput(true)
				.execute();

			Duration duration = Duration.between(startTime, Instant.now());

			String output = result.outputUTF8();
			int exitCode = result.getExitValue();

			logger.info("Amazon Q execution completed in {} ms with exit code {}", duration.toMillis(), exitCode);

			// Extract conversation ID if present in output
			String conversationId = extractConversationId(output);

			return new ExecuteResult(output, exitCode, options.getModel(), duration, conversationId);
		}
		catch (Exception e) {
			logger.error("Amazon Q execution failed", e);
			throw new AmazonQSDKException("Amazon Q CLI execution failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Builds the command line for Amazon Q CLI execution.
	 * @param prompt the prompt to execute
	 * @param options execution options
	 * @return the command as a list of strings
	 */
	private List<String> buildCommand(String prompt, ExecuteOptions options) {
		List<String> command = new ArrayList<>();
		command.add(qCliPath);
		command.add("chat");

		// Model selection
		if (options.getModel() != null) {
			command.add("--model");
			command.add(options.getModel());
		}

		// Agent/context profile
		if (options.getAgent() != null) {
			command.add("--agent");
			command.add(options.getAgent());
		}

		// Resume previous conversation
		if (options.isResume()) {
			command.add("--resume");
		}

		// Non-interactive mode (required for programmatic use)
		if (options.isNoInteractive()) {
			command.add("--no-interactive");
		}

		// Tool trust configuration
		if (options.isTrustAllTools()) {
			command.add("--trust-all-tools");
		}
		else if (options.getTrustTools() != null && !options.getTrustTools().isEmpty()) {
			command.add("--trust-tools=" + String.join(",", options.getTrustTools()));
		}

		// Verbose logging
		if (options.isVerbose()) {
			command.add("--verbose");
		}

		// Add the prompt as the last argument
		command.add(prompt);

		return command;
	}

	/**
	 * Extracts conversation ID from Q output if present.
	 * @param output the CLI output
	 * @return the conversation ID or null
	 */
	private String extractConversationId(String output) {
		// Amazon Q may include conversation metadata in output
		// For now, return null as conversation ID extraction needs to be
		// determined based on actual Q CLI behavior
		return null;
	}

}
