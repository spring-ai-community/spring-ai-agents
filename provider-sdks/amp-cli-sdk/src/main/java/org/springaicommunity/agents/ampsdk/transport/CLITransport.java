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

package org.springaicommunity.agents.ampsdk.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.ampsdk.exceptions.AmpSDKException;
import org.springaicommunity.agents.ampsdk.types.ExecuteOptions;
import org.springaicommunity.agents.ampsdk.types.ExecuteResult;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Low-level transport layer for Amp CLI communication using zt-exec for robust process
 * management.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class CLITransport {

	private static final Logger logger = LoggerFactory.getLogger(CLITransport.class);

	private final Path workingDirectory;

	private final String ampCliPath;

	public CLITransport(Path workingDirectory) {
		this(workingDirectory, null);
	}

	public CLITransport(Path workingDirectory, String ampCliPath) {
		this.workingDirectory = workingDirectory;
		this.ampCliPath = ampCliPath != null ? ampCliPath : AmpCliDiscovery.discoverAmpCli();

		// Validate the CLI is functional
		if (!AmpCliDiscovery.validateAmpCli(this.ampCliPath)) {
			throw new AmpSDKException("Amp CLI at " + this.ampCliPath + " is not functional");
		}

		logger.info("Amp CLI initialized at: {}", this.ampCliPath);
	}

	/**
	 * Execute a prompt via Amp CLI in execute mode.
	 * @param prompt the user prompt/goal to execute
	 * @param options execution options
	 * @return the execution result
	 * @throws AmpSDKException if execution fails
	 */
	public ExecuteResult execute(String prompt, ExecuteOptions options) {
		if (prompt == null || prompt.isEmpty()) {
			throw new IllegalArgumentException("Prompt cannot be null or empty");
		}
		if (options == null) {
			options = ExecuteOptions.defaultOptions();
		}

		List<String> command = buildCommand(prompt, options);
		Instant startTime = Instant.now();

		logger.debug("Executing Amp CLI command: {}", command);

		try {
			// Amp CLI requires prompt via stdin when using -x flag
			ProcessResult result = new ProcessExecutor().command(command)
				.directory(workingDirectory.toFile())
				.timeout(options.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
				.readOutput(true)
				.destroyOnExit()
				.redirectInput(
						new java.io.ByteArrayInputStream(prompt.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
				.execute();

			Duration duration = Duration.between(startTime, Instant.now());

			String output = result.outputUTF8();
			int exitCode = result.getExitValue();

			logger.debug("Amp CLI execution completed. Exit code: {}, Duration: {}ms", exitCode, duration.toMillis());

			// Extract model from output if available (Amp doesn't return model in simple
			// mode)
			String model = "amp-default";

			return new ExecuteResult(output, exitCode, duration, model);
		}
		catch (Exception e) {
			Duration duration = Duration.between(startTime, Instant.now());
			logger.error("Amp CLI execution failed after {}ms: {}", duration.toMillis(), e.getMessage());
			throw new AmpSDKException("Failed to execute Amp CLI command", e);
		}
	}

	private List<String> buildCommand(String prompt, ExecuteOptions options) {
		List<String> command = new ArrayList<>();

		// Base command
		command.add(ampCliPath);

		// Add options BEFORE -x flag
		// --dangerously-allow-all must come before -x
		if (options.isDangerouslyAllowAll()) {
			command.add("--dangerously-allow-all");
		}

		// Add model if specified
		// Note: Amp CLI uses environment variable for model selection, not a flag
		// We'll pass the model via environment in the future if needed

		// Execute mode flag (prompt will be provided via stdin)
		command.add("-x");

		return command;
	}

	/**
	 * Checks if the Amp CLI is available and functional.
	 * @return true if available
	 */
	public boolean checkAvailability() {
		return AmpCliDiscovery.validateAmpCli(ampCliPath);
	}

	public String getAmpCliPath() {
		return ampCliPath;
	}

}
