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

package org.springaicommunity.agents.codexsdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.codexsdk.transport.CLITransport;
import org.springaicommunity.agents.codexsdk.types.ExecuteOptions;
import org.springaicommunity.agents.codexsdk.types.ExecuteResult;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OpenAI Codex CLI client for managing Codex CLI subprocess communication. Provides
 * high-level access to the Codex CLI with rich domain objects.
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
public class CodexClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(CodexClient.class);

	private final CLITransport transport;

	private final ExecuteOptions defaultOptions;

	private CodexClient(CLITransport transport, ExecuteOptions defaultOptions) {
		this.transport = transport;
		this.defaultOptions = defaultOptions;
	}

	/**
	 * Creates a new client with default working directory and options.
	 * @return new CodexClient instance
	 */
	public static CodexClient create() {
		return create(ExecuteOptions.defaultOptions());
	}

	/**
	 * Creates a new client with specified options.
	 * @param options execution options
	 * @return new CodexClient instance
	 */
	public static CodexClient create(ExecuteOptions options) {
		return create(options, Paths.get(System.getProperty("user.dir")));
	}

	/**
	 * Creates a new client with specified options and working directory.
	 * @param options execution options
	 * @param workingDirectory working directory for Codex operations
	 * @return new CodexClient instance
	 */
	public static CodexClient create(ExecuteOptions options, Path workingDirectory) {
		return create(options, workingDirectory, null);
	}

	/**
	 * Creates a new client with specified options, working directory, and CLI path.
	 * @param options execution options
	 * @param workingDirectory working directory for Codex operations
	 * @param codexCliPath path to Codex CLI executable (null for auto-discovery)
	 * @return new CodexClient instance
	 */
	public static CodexClient create(ExecuteOptions options, Path workingDirectory, String codexCliPath) {
		CLITransport transport = new CLITransport(workingDirectory, codexCliPath);
		return new CodexClient(transport, options);
	}

	/**
	 * Execute a prompt via Codex CLI in execute mode.
	 * @param prompt the user prompt/goal to execute
	 * @return execution result
	 */
	public ExecuteResult execute(String prompt) {
		return execute(prompt, defaultOptions);
	}

	/**
	 * Execute a prompt via Codex CLI in execute mode with custom options.
	 * @param prompt the user prompt/goal to execute
	 * @param options execution options
	 * @return execution result
	 */
	public ExecuteResult execute(String prompt, ExecuteOptions options) {
		logger.debug("Executing Codex CLI with prompt: {}", prompt);
		return transport.execute(prompt, options);
	}

	/**
	 * Resume a previous Codex session with a new prompt.
	 * @param sessionId the session ID to resume
	 * @param prompt the new prompt/goal to execute
	 * @return execution result
	 */
	public ExecuteResult resume(String sessionId, String prompt) {
		return resume(sessionId, prompt, defaultOptions);
	}

	/**
	 * Resume a previous Codex session with a new prompt and custom options.
	 * @param sessionId the session ID to resume
	 * @param prompt the new prompt/goal to execute
	 * @param options execution options
	 * @return execution result
	 */
	public ExecuteResult resume(String sessionId, String prompt, ExecuteOptions options) {
		logger.debug("Resuming Codex CLI session {} with prompt: {}", sessionId, prompt);
		return transport.resume(sessionId, prompt, options);
	}

	/**
	 * Checks if the Codex CLI is available and functional.
	 * @return true if Codex CLI is available
	 */
	public boolean isAvailable() {
		try {
			return transport.checkAvailability();
		}
		catch (Exception e) {
			logger.warn("Codex CLI availability check failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Gets the path to the Codex CLI executable being used.
	 * @return path to Codex CLI
	 */
	public String getCodexCliPath() {
		return transport.getCodexCliPath();
	}

	@Override
	public void close() {
		// Cleanup if needed in the future
		logger.debug("Closing CodexClient");
	}

}
