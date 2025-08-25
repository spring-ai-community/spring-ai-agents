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

package org.springaicommunity.agents.geminisdk.transport;

import org.springaicommunity.agents.geminisdk.exceptions.CLINotFoundException;
import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.exceptions.ProcessExecutionException;
import org.springaicommunity.agents.geminisdk.exceptions.TimeoutException;
import org.springaicommunity.agents.geminisdk.types.TextMessage;
import org.springaicommunity.agents.geminisdk.types.Message;
import org.springaicommunity.agents.geminisdk.types.MessageType;
import org.springaicommunity.agents.geminisdk.util.GeminiCliDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.InvalidExitValueException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Transport layer for executing Gemini CLI commands. Handles process management and
 * response parsing.
 */
public class CLITransport {

	private static final Logger logger = LoggerFactory.getLogger(CLITransport.class);

	private final Path workingDirectory;

	private final Duration defaultTimeout;

	private final String geminiCommand;

	public CLITransport(Path workingDirectory, Duration defaultTimeout) {
		this.workingDirectory = workingDirectory;
		this.defaultTimeout = defaultTimeout;
		this.geminiCommand = GeminiCliDiscovery.findGeminiCommand();
	}

	/**
	 * Checks if the Gemini CLI is available and functional.
	 * @return true if Gemini CLI is installed, accessible, and responds to --version
	 */
	public boolean isAvailable() {
		return checkCliAvailability().isAvailable();
	}

	/**
	 * Performs a comprehensive CLI availability check. Delegates to GeminiCliDiscovery
	 * for consistent behavior.
	 * @return detailed availability information
	 */
	public CliAvailabilityResult checkCliAvailability() {
		return GeminiCliDiscovery.checkCommandAvailability(geminiCommand);
	}

	/**
	 * Gets the Gemini CLI version. Delegates to GeminiCliDiscovery for consistent
	 * behavior.
	 */
	public String getVersion() throws GeminiSDKException {
		CliAvailabilityResult result = checkCliAvailability();
		if (result.isAvailable()) {
			return result.getVersion().orElse("unknown");
		}
		else {
			Throwable cause = result.getCause().orElse(null);
			String reason = result.getReason().orElse("unknown error");
			if (cause instanceof java.util.concurrent.TimeoutException) {
				throw new TimeoutException("Timeout getting Gemini CLI version", Duration.ofSeconds(10), cause);
			}
			else if (cause instanceof IOException || cause instanceof InterruptedException) {
				throw new CLINotFoundException("Gemini CLI not found or not accessible", cause);
			}
			else {
				throw new GeminiSDKException("Failed to get Gemini CLI version: " + reason, cause);
			}
		}
	}

	/**
	 * Executes a query using the Gemini CLI.
	 */
	public List<Message> executeQuery(String prompt, CLIOptions options) throws GeminiSDKException {
		if (prompt == null || prompt.trim().isEmpty()) {
			throw new IllegalArgumentException("Prompt cannot be null or empty");
		}

		List<String> command = buildCommand(prompt, options);

		logger.info("Executing Gemini CLI command with prompt length: {}", prompt.length());
		logger.debug("Command: {}", String.join(" ", command));

		try {
			// Use GeminiCliDiscovery to get the correct command for nvm environments
			String[] commandArray = GeminiCliDiscovery.getGeminiCommand(geminiCommand,
					command.subList(1, command.size()).toArray(new String[0]));

			ProcessResult result = new ProcessExecutor().command(commandArray)
				.directory(workingDirectory.toFile())
				.timeout(options.getTimeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
				.readOutput(true)
				.exitValueNormal()
				.execute();

			String output = result.outputUTF8();
			logger.debug("Gemini CLI output length: {}", output.length());

			return parseResponse(output);

		}
		catch (InvalidExitValueException e) {
			ProcessResult result = e.getResult();
			String output = result.outputUTF8();
			String stderr = (output != null && (output.contains("Error:") || output.contains("error:"))) ? output
					: "No stderr captured";
			throw new ProcessExecutionException("Gemini CLI execution failed", e.getExitValue(), output, stderr, e);
		}
		catch (java.util.concurrent.TimeoutException e) {
			throw new TimeoutException("Gemini CLI execution timed out", options.getTimeout(), e);
		}
		catch (IOException | InterruptedException e) {
			throw new GeminiSDKException("Failed to execute Gemini CLI", e);
		}
	}

	private List<String> buildCommand(String prompt, CLIOptions options) {
		List<String> command = new ArrayList<>();
		command.add(geminiCommand);

		// Add model flag if specified
		if (options.getModel() != null && !options.getModel().trim().isEmpty()) {
			command.add("-m");
			command.add(options.getModel());
		}

		// Add yolo mode (auto-accept) for non-interactive use
		if (options.isYoloMode()) {
			command.add("-y");
		}

		// Add all files flag if requested
		if (options.isAllFiles()) {
			command.add("-a");
		}

		// Add debug flag if requested
		if (options.isDebug()) {
			command.add("-d");
		}

		// Add sandbox flag if requested
		if (options.isSandbox()) {
			command.add("-s");
		}

		// Add sandbox image if specified
		if (options.getSandboxImage() != null && !options.getSandboxImage().trim().isEmpty()) {
			command.add("--sandbox-image");
			command.add(options.getSandboxImage());
		}

		// Add proxy configuration if specified
		if (options.getProxy() != null && !options.getProxy().trim().isEmpty()) {
			command.add("--proxy");
			command.add(options.getProxy());
		}

		// Add include directories if specified
		if (!options.getIncludeDirectories().isEmpty()) {
			command.add("--include-directories");
			command.add(String.join(",", options.getIncludeDirectories()));
		}

		// Add extensions if specified
		if (!options.getExtensions().isEmpty()) {
			command.add("-e");
			// Extensions can be specified multiple times or comma-separated
			for (String extension : options.getExtensions()) {
				command.add(extension);
			}
		}

		// Add prompt (must be last parameter for CLI compatibility)
		command.add("-p");
		command.add(prompt);

		return command;
	}

	private List<Message> parseResponse(String output) {
		List<Message> messages = new ArrayList<>();

		if (output == null || output.trim().isEmpty()) {
			messages.add(TextMessage.error("Empty response from Gemini CLI"));
			return messages;
		}

		// For now, treat the entire output as a single assistant message
		// Future versions could implement more sophisticated parsing
		messages.add(TextMessage.assistant(output.trim()));

		return messages;
	}

}