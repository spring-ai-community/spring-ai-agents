/*
 * Copyright 2025 Spring AI Community
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

package org.springaicommunity.agents.claude.sdk.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.claude.sdk.config.ClaudeCliDiscovery;
import org.springaicommunity.agents.claude.sdk.config.OutputFormat;
import org.springaicommunity.agents.claude.sdk.exceptions.CLIConnectionException;
import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.exceptions.ProcessExecutionException;
import org.springaicommunity.agents.claude.sdk.parsing.ControlMessageParser;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlRequest;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;
import org.springaicommunity.agents.model.sandbox.ExecSpec;
import org.springaicommunity.agents.model.sandbox.Sandbox;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Bidirectional transport for Claude CLI control protocol communication. This transport
 * enables hook callbacks and permission handling through stdin/stdout communication.
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Uses --input-format stream-json for sending messages to CLI</li>
 * <li>Uses --output-format stream-json for receiving messages</li>
 * <li>Uses --permission-prompt-tool stdio for bidirectional control</li>
 * <li>Handles both regular messages and control requests</li>
 * <li>Thread-safe response writing</li>
 * </ul>
 *
 * @see ControlMessageParser
 * @see ControlRequest
 * @see ControlResponse
 */
public class BidirectionalTransport implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(BidirectionalTransport.class);

	private final String claudeCommand;

	private final Path workingDirectory;

	private final Duration defaultTimeout;

	private final Sandbox sandbox;

	private final ControlMessageParser parser;

	private final ObjectMapper objectMapper;

	private final ExecutorService executor;

	// Process management
	private volatile Process process;

	private volatile BufferedWriter stdinWriter;

	private volatile BufferedReader stdoutReader;

	private volatile BufferedReader stderrReader;

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final AtomicReference<Throwable> error = new AtomicReference<>();

	// Synchronization for stdin writes
	private final Object stdinLock = new Object();

	public BidirectionalTransport(Path workingDirectory) {
		this(workingDirectory, Duration.ofMinutes(10), null, null);
	}

	public BidirectionalTransport(Path workingDirectory, Duration defaultTimeout) {
		this(workingDirectory, defaultTimeout, null, null);
	}

	public BidirectionalTransport(Path workingDirectory, Duration defaultTimeout, String claudePath) {
		this(workingDirectory, defaultTimeout, claudePath, null);
	}

	/**
	 * Creates a BidirectionalTransport with an optional Sandbox for process execution.
	 * @param workingDirectory the working directory for the CLI
	 * @param defaultTimeout default timeout for operations
	 * @param claudePath optional path to Claude CLI executable
	 * @param sandbox optional Sandbox for process execution (enables Docker support)
	 */
	public BidirectionalTransport(Path workingDirectory, Duration defaultTimeout, String claudePath, Sandbox sandbox) {
		this.workingDirectory = workingDirectory;
		this.defaultTimeout = defaultTimeout;
		this.claudeCommand = claudePath != null ? claudePath : discoverClaudePath();
		this.sandbox = sandbox;
		this.parser = new ControlMessageParser();
		this.objectMapper = new ObjectMapper();
		this.executor = Executors.newVirtualThreadPerTaskExecutor();
	}

	private String discoverClaudePath() {
		try {
			return ClaudeCliDiscovery.discoverClaudePath();
		}
		catch (Exception e) {
			logger.warn("Could not discover Claude CLI path, using 'claude'", e);
			return "claude";
		}
	}

	/**
	 * Starts a bidirectional session with the Claude CLI.
	 * @param prompt the initial prompt
	 * @param options CLI options (will be modified for bidirectional mode)
	 * @param messageHandler handler for regular messages
	 * @param controlRequestHandler handler for control requests, returns response
	 * @throws ClaudeSDKException if the session fails to start
	 */
	public void startSession(String prompt, CLIOptions options, Consumer<ParsedMessage> messageHandler,
			ControlRequestHandler controlRequestHandler) throws ClaudeSDKException {

		if (running.get()) {
			throw new IllegalStateException("Session already running");
		}

		try {
			// Build command with bidirectional flags (no prompt - sent via stdin)
			List<String> command = buildBidirectionalCommand(options);

			logger.info("Starting bidirectional session: {}", command);

			// Build environment variables
			Map<String, String> env = new HashMap<>();
			env.put("CLAUDE_CODE_ENTRYPOINT", "sdk-java");
			String apiKey = System.getenv("ANTHROPIC_API_KEY");
			if (apiKey != null) {
				env.put("ANTHROPIC_API_KEY", apiKey);
			}

			// Start process using sandbox if available, otherwise direct ProcessBuilder
			if (sandbox != null) {
				ExecSpec spec = ExecSpec.builder().command(command).env(env).timeout(defaultTimeout).build();
				process = sandbox.startInteractive(spec);
			}
			else {
				// Direct process execution (local)
				ProcessBuilder pb = new ProcessBuilder(command);
				pb.directory(workingDirectory.toFile());
				pb.environment().putAll(env);
				process = pb.start();
			}
			running.set(true);

			// Setup streams
			stdinWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
			stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
			stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

			// Start stderr reader thread
			executor.submit(() -> readStderr());

			// Process stdout in the calling thread or a dedicated thread
			executor.submit(() -> processMessages(messageHandler, controlRequestHandler));

			// Send initial prompt as JSON via stdin (required for --input-format
			// stream-json)
			sendUserMessage(prompt, "default");

		}
		catch (IOException e) {
			running.set(false);
			throw new CLIConnectionException("Failed to start bidirectional session", e);
		}
		catch (Exception e) {
			running.set(false);
			throw new CLIConnectionException("Failed to start bidirectional session via sandbox", e);
		}
	}

	/**
	 * Builds the command for bidirectional mode. In bidirectional mode, the prompt is
	 * sent via stdin as JSON, not as a command-line argument. This matches the Python SDK
	 * behavior.
	 */
	List<String> buildBidirectionalCommand(CLIOptions options) {
		List<String> command = new ArrayList<>();
		command.add(claudeCommand);

		// Bidirectional mode flags (no --print, prompt sent via stdin)
		command.add("--input-format");
		command.add("stream-json");
		command.add("--output-format");
		command.add("stream-json");
		command.add("--permission-prompt-tool");
		command.add("stdio");
		command.add("--verbose");

		// Standard options
		if (options.getModel() != null) {
			command.add("--model");
			command.add(options.getModel());
		}

		if (options.getSystemPrompt() != null) {
			command.add("--append-system-prompt");
			command.add(options.getSystemPrompt());
		}

		if (!options.getAllowedTools().isEmpty()) {
			command.add("--allowed-tools");
			command.add(String.join(",", options.getAllowedTools()));
		}

		if (!options.getDisallowedTools().isEmpty()) {
			command.add("--disallowed-tools");
			command.add(String.join(",", options.getDisallowedTools()));
		}

		// Permission mode (note: we're using --permission-prompt-tool stdio for hooks)
		// Additional permission mode can still be set for base behavior
		if (options.getPermissionMode() != null) {
			command.add("--permission-mode");
			command.add(options.getPermissionMode().getValue());
		}

		// No prompt argument - it will be sent via stdin as JSON

		return command;
	}

	/**
	 * Sends a user message to the CLI via stdin. This is used to send the initial prompt
	 * and follow-up messages in bidirectional mode.
	 * @param content the message content
	 * @param sessionId the session ID (use "default" for initial session)
	 * @throws ClaudeSDKException if sending fails
	 */
	public void sendUserMessage(String content, String sessionId) throws ClaudeSDKException {
		Map<String, Object> message = new HashMap<>();
		message.put("type", "user");
		message.put("session_id", sessionId != null ? sessionId : "default");

		Map<String, Object> innerMessage = new HashMap<>();
		innerMessage.put("role", "user");
		innerMessage.put("content", content);
		message.put("message", innerMessage);

		synchronized (stdinLock) {
			try {
				if (stdinWriter == null) {
					throw new IllegalStateException("Session not started or already closed");
				}

				String json = objectMapper.writeValueAsString(message);
				logger.debug("Sending user message: {}", json);

				stdinWriter.write(json);
				stdinWriter.newLine();
				stdinWriter.flush();
			}
			catch (IOException e) {
				throw new ProcessExecutionException("Failed to send user message to CLI", e);
			}
		}
	}

	/**
	 * Processes messages from stdout, dispatching to appropriate handlers.
	 */
	private void processMessages(Consumer<ParsedMessage> messageHandler, ControlRequestHandler controlRequestHandler) {
		try {
			String line;
			while (running.get() && (line = stdoutReader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				try {
					ParsedMessage parsed = parser.parse(line);

					if (parsed.isControlRequest()) {
						// Handle control request
						ControlRequest request = parsed.asControlRequest();
						logger.debug("Received control request: type={}, requestId={}",
								request.request() != null ? request.request().subtype() : "null", request.requestId());

						// Get response from handler
						ControlResponse response = controlRequestHandler.handle(request);

						// Send response back to CLI
						sendResponse(response);
					}
					else {
						// Regular message - pass to handler
						messageHandler.accept(parsed);
					}
				}
				catch (Exception e) {
					logger.warn("Failed to process message: {}", line.substring(0, Math.min(100, line.length())), e);
				}
			}

			logger.debug("Message processing loop ended");
		}
		catch (IOException e) {
			if (running.get()) {
				error.set(e);
				logger.error("Error reading from stdout", e);
			}
		}
		finally {
			running.set(false);
		}
	}

	/**
	 * Sends a control response back to the CLI via stdin.
	 * @param response the response to send
	 * @throws ClaudeSDKException if sending fails
	 */
	public void sendResponse(ControlResponse response) throws ClaudeSDKException {
		synchronized (stdinLock) {
			try {
				if (stdinWriter == null) {
					throw new IllegalStateException("Session not started or already closed");
				}

				String json = objectMapper.writeValueAsString(response);
				logger.debug("Sending control response: {}", json);

				stdinWriter.write(json);
				stdinWriter.newLine();
				stdinWriter.flush();
			}
			catch (IOException e) {
				throw new ProcessExecutionException("Failed to send response to CLI", e);
			}
		}
	}

	/**
	 * Sends a message to the CLI via stdin (for user input in ongoing sessions).
	 * @param message the message JSON to send
	 * @throws ClaudeSDKException if sending fails
	 */
	public void sendMessage(String message) throws ClaudeSDKException {
		synchronized (stdinLock) {
			try {
				if (stdinWriter == null) {
					throw new IllegalStateException("Session not started or already closed");
				}

				logger.debug("Sending message: {}", message);

				stdinWriter.write(message);
				stdinWriter.newLine();
				stdinWriter.flush();
			}
			catch (IOException e) {
				throw new ProcessExecutionException("Failed to send message to CLI", e);
			}
		}
	}

	/**
	 * Reads and logs stderr output.
	 */
	private void readStderr() {
		try {
			String line;
			while (running.get() && (line = stderrReader.readLine()) != null) {
				logger.warn("CLI stderr: {}", line);
			}
		}
		catch (IOException e) {
			if (running.get()) {
				logger.debug("Error reading stderr", e);
			}
		}
	}

	/**
	 * Waits for the session to complete.
	 * @param timeout maximum time to wait
	 * @return true if completed within timeout, false otherwise
	 * @throws ClaudeSDKException if the session failed with an error
	 */
	public boolean waitForCompletion(Duration timeout) throws ClaudeSDKException {
		if (process == null) {
			return true;
		}

		try {
			boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

			if (completed) {
				int exitCode = process.exitValue();
				if (exitCode != 0) {
					throw ProcessExecutionException.withExitCode("CLI process failed", exitCode);
				}
			}

			Throwable err = error.get();
			if (err != null) {
				throw new ProcessExecutionException("Session error", err);
			}

			return completed;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ProcessExecutionException("Wait interrupted", e);
		}
	}

	/**
	 * Checks if the session is currently running.
	 */
	public boolean isRunning() {
		return running.get() && process != null && process.isAlive();
	}

	/**
	 * Interrupts the current session.
	 */
	public void interrupt() {
		running.set(false);
		if (process != null) {
			process.destroy();
		}
	}

	@Override
	public void close() {
		running.set(false);

		// Close streams
		closeQuietly(stdinWriter);
		closeQuietly(stdoutReader);
		closeQuietly(stderrReader);

		// Terminate process
		if (process != null) {
			process.destroy();
			try {
				if (!process.waitFor(5, TimeUnit.SECONDS)) {
					process.destroyForcibly();
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				process.destroyForcibly();
			}
		}

		// Shutdown executor
		executor.shutdown();
		try {
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			executor.shutdownNow();
		}

		logger.debug("BidirectionalTransport closed");
	}

	private void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			}
			catch (IOException e) {
				logger.debug("Error closing stream", e);
			}
		}
	}

	/**
	 * Functional interface for handling control requests.
	 */
	@FunctionalInterface
	public interface ControlRequestHandler {

		/**
		 * Handles a control request and returns a response.
		 * @param request the control request from CLI
		 * @return the response to send back
		 */
		ControlResponse handle(ControlRequest request);

	}

}
