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

package org.springaicommunity.agents.sweagentsdk.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.sweagentsdk.exceptions.SweCliNotFoundException;
import org.springaicommunity.agents.sweagentsdk.types.SweAgentOptions;
import org.springaicommunity.agents.sweagentsdk.util.SweCliDiscovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API wrapper for the mini-SWE-agent CLI tool.
 *
 * <p>
 * This class provides a high-level interface to the mini-swe-agent CLI, handling CLI
 * discovery, process execution, and result parsing. It automatically discovers the CLI
 * executable and provides robust error handling.
 * </p>
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class SweCliApi {

	private static final Logger logger = LoggerFactory.getLogger(SweCliApi.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final String executablePath;

	/**
	 * Creates a new SweCliApi with automatic CLI discovery.
	 * @throws SweCliNotFoundException if the CLI cannot be found
	 */
	public SweCliApi() {
		String discoveredPath = SweCliDiscovery.getDiscoveredPath();
		if (discoveredPath == null) {
			throw new SweCliNotFoundException(
					"mini-swe-agent CLI not found. Please ensure it is installed and available.");
		}
		this.executablePath = discoveredPath;
		logger.debug("SweCliApi initialized with discovered CLI path: {}", this.executablePath);
	}

	/**
	 * Creates a new SweCliApi with the specified executable path.
	 * @param executablePath the path to the mini-swe-agent CLI executable
	 */
	public SweCliApi(String executablePath) {
		this.executablePath = executablePath != null ? executablePath : SweCliDiscovery.findSweCommand();
		logger.debug("SweCliApi initialized with executable path: {}", this.executablePath);
	}

	/**
	 * Checks if the SWE Agent CLI is available and functional.
	 * @return true if the CLI is available, false otherwise
	 */
	public boolean isAvailable() {
		try {
			return SweCliDiscovery.isCommandAvailable(this.executablePath);
		}
		catch (Exception e) {
			logger.debug("CLI availability check failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Execute a task using the mini-SWE-agent CLI.
	 * @param prompt the task description/prompt
	 * @param workingDirectory the working directory for execution
	 * @param options the agent options
	 * @return the execution result
	 * @throws SweCliException if execution fails
	 */
	public SweResult execute(String prompt, Path workingDirectory, SweAgentOptions options) throws SweCliException {
		try {
			// Create temporary output file for JSON trajectory
			Path outputFile = workingDirectory.resolve("swe-agent-output-" + System.currentTimeMillis() + ".json");

			List<String> command = buildCommand(prompt, options, outputFile);
			logger.debug("Executing mini-SWE-agent with command: {}", command);

			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(workingDirectory.toFile());

			// Set environment variables if provided
			if (options.getEnvironmentVariables() != null) {
				processBuilder.environment().putAll(options.getEnvironmentVariables());
			}

			Process process = processBuilder.start();

			// Provide empty input to mini-swe-agent prompts
			// This prevents hanging when the agent prompts for confirmation or new tasks
			try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
				// Send multiple empty lines to respond to various prompts
				// (task confirmation, exit confirmation, new task prompts, etc.)
				writer.write("\n\n\n");
				writer.flush();
			}
			catch (IOException e) {
				logger.debug("Failed to write to process stdin: {}", e.getMessage());
			}

			// Handle timeout
			Duration timeout = options.getTimeout();
			boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);

			if (!finished) {
				process.destroyForcibly();
				throw new SweCliException("mini-SWE-agent execution timed out after " + timeout);
			}

			int exitCode = process.exitValue();
			String output = readOutput(process);
			String error = readError(process);

			logger.debug("mini-SWE-agent completed with exit code: {}", exitCode);

			// Try to read JSON trajectory file if it exists
			JsonNode trajectoryJson = null;
			if (Files.exists(outputFile)) {
				try {
					String trajectoryContent = Files.readString(outputFile);
					trajectoryJson = objectMapper.readTree(trajectoryContent);
					// Clean up temporary file
					Files.deleteIfExists(outputFile);
				}
				catch (Exception e) {
					logger.debug("Failed to read trajectory file: {}", e.getMessage());
				}
			}

			return parseResult(output, error, exitCode, trajectoryJson);

		}
		catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SweCliException("Failed to execute mini-SWE-agent: " + e.getMessage(), e);
		}
	}

	private List<String> buildCommand(String prompt, SweAgentOptions options, Path outputFile) {
		List<String> command = new ArrayList<>();
		command.add(executablePath);

		// Add model if specified
		if (options.getModel() != null) {
			command.add("--model");
			command.add(options.getModel());
		}

		// Add task argument
		command.add("--task");
		command.add(prompt);

		// Add output file for JSON trajectory
		command.add("--output");
		command.add(outputFile.toString());

		// Add yolo flag to skip confirmations
		command.add("--yolo");

		// Add exit-immediately flag to avoid infinite loops
		command.add("--exit-immediately");

		// Add cost limit to prevent infinite loops (as a backup to exit-immediately)
		command.add("--cost-limit");
		command.add("0.10"); // Very low cost limit to force early termination

		return command;
	}

	private String readOutput(Process process) throws IOException {
		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
		}
		return output.toString().trim();
	}

	private String readError(Process process) throws IOException {
		StringBuilder error = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				error.append(line).append("\n");
			}
		}
		return error.toString().trim();
	}

	private SweResult parseResult(String output, String error, int exitCode, JsonNode trajectoryJson) {
		// Determine status based on trajectory content if available
		SweResultStatus status;
		String resultOutput = output;

		if (trajectoryJson != null) {
			// Extract meaningful information from trajectory
			if (trajectoryJson.has("summary")) {
				resultOutput = trajectoryJson.get("summary").asText();
			}
			// Consider it successful if we have trajectory data and no explicit error
			status = (exitCode == 0 || trajectoryJson.has("steps")) ? SweResultStatus.SUCCESS : SweResultStatus.ERROR;
		}
		else {
			// Fallback to exit code
			status = exitCode == 0 ? SweResultStatus.SUCCESS : SweResultStatus.ERROR;
		}

		return new SweResult(status, resultOutput, error, trajectoryJson);
	}

	/**
	 * Result from mini-SWE-agent execution.
	 */
	public static class SweResult {

		private final SweResultStatus status;

		private final String output;

		private final String error;

		private final JsonNode metadata;

		public SweResult(SweResultStatus status, String output, String error, JsonNode metadata) {
			this.status = status;
			this.output = output;
			this.error = error;
			this.metadata = metadata;
		}

		public SweResultStatus getStatus() {
			return status;
		}

		public String getOutput() {
			return output;
		}

		public String getError() {
			return error;
		}

		public JsonNode getMetadata() {
			return metadata;
		}

		public static SweResult fromJson(JsonNode json, int exitCode) {
			SweResultStatus status = exitCode == 0 ? SweResultStatus.SUCCESS : SweResultStatus.ERROR;

			// Extract key fields from JSON
			String output = json.has("output") ? json.get("output").asText() : json.toString();
			String error = json.has("error") ? json.get("error").asText() : "";

			// Check for success indicators in the JSON
			if (json.has("success") && !json.get("success").asBoolean()) {
				status = SweResultStatus.ERROR;
			}

			return new SweResult(status, output, error, json);
		}

	}

	/**
	 * Status of mini-SWE-agent execution.
	 */
	public enum SweResultStatus {

		SUCCESS, ERROR

	}

	/**
	 * Exception thrown when mini-SWE-agent execution fails.
	 */
	public static class SweCliException extends Exception {

		public SweCliException(String message) {
			super(message);
		}

		public SweCliException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}