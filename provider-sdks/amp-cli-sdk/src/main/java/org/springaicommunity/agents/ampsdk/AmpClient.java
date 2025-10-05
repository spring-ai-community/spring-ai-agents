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

package org.springaicommunity.agents.ampsdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.ampsdk.exceptions.AmpSDKException;
import org.springaicommunity.agents.ampsdk.transport.CLITransport;
import org.springaicommunity.agents.ampsdk.types.ExecuteOptions;
import org.springaicommunity.agents.ampsdk.types.ExecuteResult;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sourcegraph Amp CLI client for managing Amp CLI subprocess communication. Provides
 * high-level access to the Amp CLI with rich domain objects.
 *
 * <p>
 * This client follows modern SDK naming conventions (e.g., AWS S3Client, Google
 * BigQueryClient) rather than Spring AI's legacy *Api naming for better developer
 * experience.
 * </p>
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class AmpClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(AmpClient.class);

	private final CLITransport transport;

	private final ExecuteOptions defaultOptions;

	private AmpClient(CLITransport transport, ExecuteOptions defaultOptions) {
		this.transport = transport;
		this.defaultOptions = defaultOptions;
	}

	/**
	 * Creates a new client with default working directory and options.
	 * @return new AmpClient instance
	 */
	public static AmpClient create() {
		return create(ExecuteOptions.defaultOptions());
	}

	/**
	 * Creates a new client with specified options.
	 * @param options execution options
	 * @return new AmpClient instance
	 */
	public static AmpClient create(ExecuteOptions options) {
		return create(options, Paths.get(System.getProperty("user.dir")));
	}

	/**
	 * Creates a new client with specified options and working directory.
	 * @param options execution options
	 * @param workingDirectory working directory for Amp operations
	 * @return new AmpClient instance
	 */
	public static AmpClient create(ExecuteOptions options, Path workingDirectory) {
		return create(options, workingDirectory, null);
	}

	/**
	 * Creates a new client with specified options, working directory, and CLI path.
	 * @param options execution options
	 * @param workingDirectory working directory for Amp operations
	 * @param ampCliPath path to Amp CLI executable (null for auto-discovery)
	 * @return new AmpClient instance
	 */
	public static AmpClient create(ExecuteOptions options, Path workingDirectory, String ampCliPath) {
		CLITransport transport = new CLITransport(workingDirectory, ampCliPath);
		return new AmpClient(transport, options);
	}

	/**
	 * Execute a prompt via Amp CLI in execute mode.
	 * @param prompt the user prompt/goal to execute
	 * @return execution result
	 */
	public ExecuteResult execute(String prompt) {
		return execute(prompt, defaultOptions);
	}

	/**
	 * Execute a prompt via Amp CLI in execute mode with custom options.
	 * @param prompt the user prompt/goal to execute
	 * @param options execution options
	 * @return execution result
	 */
	public ExecuteResult execute(String prompt, ExecuteOptions options) {
		logger.debug("Executing Amp CLI with prompt: {}", prompt);
		return transport.execute(prompt, options);
	}

	/**
	 * Checks if the Amp CLI is available and functional.
	 * @return true if Amp CLI is available
	 */
	public boolean isAvailable() {
		try {
			return transport.checkAvailability();
		}
		catch (Exception e) {
			logger.warn("Amp CLI availability check failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Gets the path to the Amp CLI executable being used.
	 * @return path to Amp CLI
	 */
	public String getAmpCliPath() {
		return transport.getAmpCliPath();
	}

	@Override
	public void close() {
		// Cleanup if needed in the future
		logger.debug("Closing AmpClient");
	}

}
