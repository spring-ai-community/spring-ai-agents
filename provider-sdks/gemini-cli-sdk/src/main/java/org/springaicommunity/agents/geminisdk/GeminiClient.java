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

package org.springaicommunity.agents.geminisdk;

import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.geminisdk.transport.CLITransport;
import org.springaicommunity.agents.geminisdk.transport.CliAvailabilityResult;
import org.springaicommunity.agents.geminisdk.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Gemini CLI client for managing Gemini CLI subprocess communication. Provides high-level
 * access to the Gemini CLI with rich domain objects.
 *
 * <p>
 * This client follows modern SDK naming conventions (e.g., AWS S3Client, Google
 * BigQueryClient) rather than Spring AI's legacy *Api naming for better developer
 * experience.
 * </p>
 */
public class GeminiClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

	private final CLITransport transport;

	private final CLIOptions defaultOptions;

	private volatile boolean connected = false;

	private GeminiClient(CLITransport transport, CLIOptions defaultOptions) {
		this.transport = transport;
		this.defaultOptions = defaultOptions;
	}

	/**
	 * Creates a new client with default working directory and options.
	 */
	public static GeminiClient create() throws GeminiSDKException {
		return create(CLIOptions.defaultOptions());
	}

	/**
	 * Creates a new client with specified options.
	 */
	public static GeminiClient create(CLIOptions options) throws GeminiSDKException {
		return create(options, Paths.get(System.getProperty("user.dir")));
	}

	/**
	 * Creates a new client with specified options and working directory.
	 */
	public static GeminiClient create(CLIOptions options, Path workingDirectory) throws GeminiSDKException {
		CLITransport transport = new CLITransport(workingDirectory, options.getTimeout());
		return new GeminiClient(transport, options);
	}

	/**
	 * Connects to Gemini CLI and validates availability.
	 */
	public void connect() throws GeminiSDKException {
		connect(false);
	}

	/**
	 * Connects to Gemini CLI with optional availability check skip.
	 * @param skipAvailabilityCheck if true, skips CLI availability validation (useful in
	 * tests)
	 */
	public void connect(boolean skipAvailabilityCheck) throws GeminiSDKException {
		logger.info("Connecting to Gemini CLI");

		if (!skipAvailabilityCheck) {
			CliAvailabilityResult availability = transport.checkCliAvailability();
			if (!availability.isAvailable()) {
				String message = availability.getReason().orElse("Unknown reason");
				Exception cause = availability.getCause().orElse(null);
				throw new GeminiSDKException("Gemini CLI is not available: " + message, cause);
			}
			String version = availability.getVersion().orElse("unknown");
			logger.info("Connected to Gemini CLI version: {}", version);
		}
		else {
			logger.info("Connected to Gemini CLI (availability check skipped)");
		}

		connected = true;
	}

	/**
	 * Executes a synchronous query and returns the complete result.
	 */
	public QueryResult query(String prompt) throws GeminiSDKException {
		return query(prompt, defaultOptions);
	}

	/**
	 * Executes a synchronous query with specified options.
	 */
	public QueryResult query(String prompt, CLIOptions options) throws GeminiSDKException {
		validateConnected();

		if (prompt == null || prompt.trim().isEmpty()) {
			throw new IllegalArgumentException("Prompt cannot be null or empty");
		}

		logger.info("Executing query with prompt length: {}", prompt.length());

		Instant startTime = Instant.now();
		List<Message> messages = transport.executeQuery(prompt, options);
		Instant endTime = Instant.now();

		Duration duration = Duration.between(startTime, endTime);
		Metadata metadata = buildMetadata(options, duration, messages);

		return QueryResult.of(messages, metadata, ResultStatus.SUCCESS);
	}

	/**
	 * Simple query method that returns just the response text.
	 */
	public String queryText(String prompt) throws GeminiSDKException {
		QueryResult result = query(prompt);
		return result.getResponse().orElse("");
	}

	/**
	 * Simple query method with options that returns just the response text.
	 */
	public String queryText(String prompt, CLIOptions options) throws GeminiSDKException {
		QueryResult result = query(prompt, options);
		return result.getResponse().orElse("");
	}

	/**
	 * Builds the command line arguments for a query without executing it. This is useful
	 * for integrating with external execution environments like Docker containers.
	 * @param prompt the query prompt
	 * @return the command line arguments as a list
	 */
	public List<String> buildCommand(String prompt) {
		return buildCommand(prompt, defaultOptions);
	}

	/**
	 * Builds the command line arguments for a query with specified options without
	 * executing it. This is useful for integrating with external execution environments.
	 * @param prompt the query prompt
	 * @param options the CLI options
	 * @return the command line arguments as a list
	 */
	public List<String> buildCommand(String prompt, CLIOptions options) {
		// Delegate to transport's buildCommand method
		return transport.buildCommand(prompt, options);
	}

	/**
	 * Parses the output from external command execution into a QueryResult. This is
	 * useful for integrating with external execution environments.
	 * @param output the command output to parse
	 * @param options the CLI options used for the command
	 * @return the parsed query result
	 * @throws GeminiSDKException if parsing fails
	 */
	public QueryResult parseResult(String output, CLIOptions options) throws GeminiSDKException {
		// Parse the output into messages using the transport's parsing logic
		List<Message> messages = transport.parseOutput(output, options);

		// Create metadata for the parsed result
		Metadata metadata = Metadata.builder()
			.model(options.getModel() != null ? options.getModel() : "gemini-default")
			.timestamp(Instant.now())
			.duration(Duration.ZERO) // Duration not available from external execution
			.build();

		return QueryResult.of(messages, metadata, ResultStatus.SUCCESS);
	}

	/**
	 * Returns the default CLI options used by this client.
	 * @return the default CLIOptions
	 */
	public CLIOptions getOptions() {
		return this.defaultOptions;
	}

	private void validateConnected() throws GeminiSDKException {
		if (!connected) {
			throw new GeminiSDKException("Not connected to Gemini CLI. Call connect() first.");
		}
	}

	private Metadata buildMetadata(CLIOptions options, Duration duration, List<Message> messages) {
		return Metadata.builder()
			.model(options.getModel() != null ? options.getModel() : "gemini-default")
			.timestamp(Instant.now())
			.duration(duration)
			.build();
	}

	@Override
	public void close() {
		logger.info("Closing Gemini CLI client");
		connected = false;
	}

}