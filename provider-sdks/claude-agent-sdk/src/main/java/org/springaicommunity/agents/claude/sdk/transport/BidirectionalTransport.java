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
import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.exceptions.SessionClosedException;
import org.springaicommunity.agents.claude.sdk.exceptions.TransportException;
import org.springaicommunity.agents.claude.sdk.parsing.ControlMessageParser;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlRequest;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;
import org.springaicommunity.agents.model.sandbox.ExecSpec;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
 * <li>Thread-safe response writing with scheduler separation</li>
 * <li>Explicit state machine for lifecycle management</li>
 * <li>Reactive Sinks for message buffering with backpressure</li>
 * <li>Iterator-based API for non-reactive consumers</li>
 * </ul>
 *
 * @see ControlMessageParser
 * @see ControlRequest
 * @see ControlResponse
 */
public class BidirectionalTransport implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(BidirectionalTransport.class);

	// ============================================================
	// State Machine Constants
	// ============================================================

	/** Transport is created but not connected */
	public static final int STATE_DISCONNECTED = 0;

	/** Connection in progress */
	public static final int STATE_CONNECTING = 1;

	/** Connected and ready for communication */
	public static final int STATE_CONNECTED = 2;

	/** Graceful shutdown in progress */
	public static final int STATE_CLOSING = 3;

	/** Fully closed */
	public static final int STATE_CLOSED = 4;

	// ============================================================
	// Configuration
	// ============================================================

	private final String claudeCommand;

	private final Path workingDirectory;

	private final Duration defaultTimeout;

	private final Sandbox sandbox;

	private final ControlMessageParser parser;

	private final ObjectMapper objectMapper;

	// ============================================================
	// State Management (Atomic State Machine)
	// ============================================================

	private final AtomicInteger state = new AtomicInteger(STATE_DISCONNECTED);

	private final AtomicReference<Throwable> sessionError = new AtomicReference<>();

	private final AtomicReference<String> sessionId = new AtomicReference<>();

	/** Flag for clean shutdown - volatile for visibility across threads (MCP pattern) */
	private volatile boolean isClosing = false;

	// ============================================================
	// Scheduler Separation (from MCP SDK pattern)
	// ============================================================

	private final Scheduler inboundScheduler;

	private final Scheduler outboundScheduler;

	private final Scheduler errorScheduler;

	// ============================================================
	// Reactive Sinks for Message Buffering
	// ============================================================

	private final Sinks.Many<ParsedMessage> inboundSink;

	private final Sinks.Many<String> outboundSink;

	private final Sinks.One<Map<String, Object>> serverInfoSink;

	// ============================================================
	// Resource Tracking (Disposable.Composite pattern)
	// ============================================================

	private final Disposable.Composite subscriptions = Disposables.composite();

	// ============================================================
	// Process Management
	// ============================================================

	private volatile Process process;

	private volatile BufferedWriter stdinWriter;

	private volatile BufferedReader stdoutReader;

	private volatile BufferedReader stderrReader;

	// Synchronization for stdin writes (belt-and-suspenders with outbound scheduler)
	private final Object stdinLock = new Object();

	// ============================================================
	// Constructors
	// ============================================================

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
	 * @param claudePath optional path to Claude CLI executable (auto-discovers if null)
	 * @param sandbox optional Sandbox for process execution (enables Docker support)
	 * @throws IllegalArgumentException if workingDirectory or defaultTimeout is null
	 */
	public BidirectionalTransport(Path workingDirectory, Duration defaultTimeout, String claudePath, Sandbox sandbox) {
		// MCP SDK pattern: strict validation for required arguments
		if (workingDirectory == null) {
			throw new IllegalArgumentException("workingDirectory must not be null");
		}
		if (defaultTimeout == null) {
			throw new IllegalArgumentException("defaultTimeout must not be null");
		}
		this.workingDirectory = workingDirectory;
		this.defaultTimeout = defaultTimeout;
		this.claudeCommand = claudePath != null ? claudePath : discoverClaudePath();
		this.sandbox = sandbox;
		this.parser = new ControlMessageParser();
		this.objectMapper = new ObjectMapper();

		// Initialize schedulers with named threads for debugging
		this.inboundScheduler = Schedulers
			.fromExecutorService(Executors.newSingleThreadExecutor(r -> new Thread(r, "claude-inbound")), "inbound");
		this.outboundScheduler = Schedulers
			.fromExecutorService(Executors.newSingleThreadExecutor(r -> new Thread(r, "claude-outbound")), "outbound");
		this.errorScheduler = Schedulers
			.fromExecutorService(Executors.newSingleThreadExecutor(r -> new Thread(r, "claude-error")), "error");

		// Initialize sinks with backpressure
		this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
		this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();
		this.serverInfoSink = Sinks.one();
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

	// ============================================================
	// State Machine Methods
	// ============================================================

	/**
	 * Gets the current state of the transport.
	 */
	public int getState() {
		return state.get();
	}

	/**
	 * Gets the state name for logging/debugging.
	 */
	public String getStateName() {
		return switch (state.get()) {
			case STATE_DISCONNECTED -> "DISCONNECTED";
			case STATE_CONNECTING -> "CONNECTING";
			case STATE_CONNECTED -> "CONNECTED";
			case STATE_CLOSING -> "CLOSING";
			case STATE_CLOSED -> "CLOSED";
			default -> "UNKNOWN";
		};
	}

	/**
	 * Attempts a state transition. Returns true if successful.
	 */
	private boolean transitionTo(int expectedState, int newState) {
		boolean success = state.compareAndSet(expectedState, newState);
		if (success) {
			logger.debug("State transition: {} -> {}", getStateName(expectedState), getStateName(newState));
		}
		return success;
	}

	private String getStateName(int stateValue) {
		return switch (stateValue) {
			case STATE_DISCONNECTED -> "DISCONNECTED";
			case STATE_CONNECTING -> "CONNECTING";
			case STATE_CONNECTED -> "CONNECTED";
			case STATE_CLOSING -> "CLOSING";
			case STATE_CLOSED -> "CLOSED";
			default -> "UNKNOWN";
		};
	}

	// ============================================================
	// Session Lifecycle
	// ============================================================

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
		startSession(prompt, options, messageHandler, controlRequestHandler, null);
	}

	/**
	 * Starts a bidirectional session with the Claude CLI.
	 * @param prompt the initial prompt
	 * @param options CLI options (will be modified for bidirectional mode)
	 * @param messageHandler handler for regular messages
	 * @param controlRequestHandler handler for control requests, returns response
	 * @param controlResponseHandler handler for control responses to our outgoing
	 * requests
	 * @throws ClaudeSDKException if the session fails to start
	 */
	public void startSession(String prompt, CLIOptions options, Consumer<ParsedMessage> messageHandler,
			ControlRequestHandler controlRequestHandler, Consumer<ControlResponse> controlResponseHandler)
			throws ClaudeSDKException {

		// State transition: DISCONNECTED -> CONNECTING
		if (!transitionTo(STATE_DISCONNECTED, STATE_CONNECTING)) {
			int currentState = state.get();
			if (currentState == STATE_CLOSED) {
				throw new IllegalStateException("Transport has been closed and cannot be reused");
			}
			throw new IllegalStateException("Cannot start session in state: " + getStateName());
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

			// Setup streams
			stdinWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
			stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
			stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

			// Start inbound message processing on dedicated scheduler (MCP pattern)
			// Using scheduler.schedule() instead of Mono.subscribeOn() for immediate
			// execution
			inboundScheduler
				.schedule(() -> processInboundMessages(messageHandler, controlRequestHandler, controlResponseHandler));

			// Start stderr reader on dedicated scheduler
			errorScheduler.schedule(this::readStderr);

			// Start outbound message processing
			Disposable outboundDisposable = outboundSink.asFlux()
				.publishOn(outboundScheduler)
				.doOnNext(this::writeToStdin)
				.subscribe();
			subscriptions.add(outboundDisposable);

			// State transition: CONNECTING -> CONNECTED
			if (!transitionTo(STATE_CONNECTING, STATE_CONNECTED)) {
				throw new TransportException("Failed to complete connection - unexpected state change");
			}

			// Send initial prompt as JSON via stdin (if provided)
			// Prompt may be null if caller wants to send initialize request first
			if (prompt != null) {
				sendUserMessage(prompt, "default");
			}

		}
		catch (Exception e) {
			// Revert state on failure
			state.set(STATE_DISCONNECTED);
			sessionError.set(e);
			if (e instanceof ClaudeSDKException) {
				throw (ClaudeSDKException) e;
			}
			throw new TransportException("Failed to start bidirectional session", e);
		}
	}

	/**
	 * Builds the command for bidirectional mode. In bidirectional mode, the prompt is
	 * sent via stdin as JSON, not as a command-line argument.
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

		if (options.getPermissionMode() != null) {
			command.add("--permission-mode");
			command.add(options.getPermissionMode().getValue());
		}

		return command;
	}

	// ============================================================
	// Message Processing
	// ============================================================

	/**
	 * Processes inbound messages from stdout, dispatching to appropriate handlers.
	 */
	private void processInboundMessages(Consumer<ParsedMessage> messageHandler,
			ControlRequestHandler controlRequestHandler, Consumer<ControlResponse> controlResponseHandler) {
		try {
			String line;
			// Use isClosing flag for clean shutdown (MCP pattern)
			while (!isClosing && (line = stdoutReader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				try {
					ParsedMessage parsed = parser.parse(line);

					// Emit to sink for reactive consumers
					if (!inboundSink.tryEmitNext(parsed).isSuccess()) {
						if (!isClosing) {
							logger.error("Failed to emit inbound message");
						}
						break;
					}

					if (parsed.isControlRequest()) {
						// Handle control request from CLI (hooks, can_use_tool, etc.)
						ControlRequest request = parsed.asControlRequest();
						logger.debug("Received control request: type={}, requestId={}",
								request.request() != null ? request.request().subtype() : "null", request.requestId());

						// Get response from handler
						ControlResponse response = controlRequestHandler.handle(request);

						// Send response back to CLI via outbound sink
						sendResponse(response);
					}
					else if (parsed.isControlResponse()) {
						// Handle control response to our outgoing request
						ControlResponse response = parsed.asControlResponse();
						String requestId = response.response() != null ? response.response().requestId() : null;
						String subtype = response.response() != null ? response.response().subtype() : null;
						logger.debug("Received control response: requestId={}, subtype={}", requestId, subtype);

						// Route to response handler if provided
						if (controlResponseHandler != null) {
							controlResponseHandler.accept(response);
						}
					}
					else {
						// Regular message - pass to handler
						messageHandler.accept(parsed);
					}
				}
				catch (Exception e) {
					if (!isClosing) {
						logger.warn("Failed to process message: {}", line.substring(0, Math.min(100, line.length())),
								e);
					}
					break;
				}
			}

			logger.debug("Message processing loop ended");
		}
		catch (IOException e) {
			if (!isClosing) {
				sessionError.set(e);
				logger.error("Error reading from stdout", e);
			}
		}
		finally {
			isClosing = true;
			inboundSink.tryEmitComplete();
		}
	}

	/**
	 * Writes a message directly to stdin. Called on the outbound scheduler.
	 */
	private void writeToStdin(String message) {
		synchronized (stdinLock) {
			try {
				if (stdinWriter == null || isClosing) {
					logger.debug("Dropping message - transport closing or closed");
					return;
				}

				stdinWriter.write(message);
				stdinWriter.newLine();
				stdinWriter.flush();
			}
			catch (IOException e) {
				logger.error("Error writing to stdin", e);
				sessionError.set(e);
			}
		}
	}

	/**
	 * Reads and logs stderr output.
	 */
	private void readStderr() {
		try {
			String line;
			while (!isClosing && (line = stderrReader.readLine()) != null) {
				logger.warn("CLI stderr: {}", line);
			}
		}
		catch (IOException e) {
			if (!isClosing) {
				logger.debug("Error reading stderr", e);
			}
		}
	}

	// ============================================================
	// Message Sending
	// ============================================================

	/**
	 * Sends a user message to the CLI via stdin.
	 * @param content the message content
	 * @param sid the session ID (use "default" for initial session)
	 * @throws ClaudeSDKException if sending fails
	 */
	public void sendUserMessage(String content, String sid) throws ClaudeSDKException {
		assertConnected();

		Map<String, Object> message = new HashMap<>();
		message.put("type", "user");
		message.put("session_id", sid != null ? sid : "default");

		Map<String, Object> innerMessage = new HashMap<>();
		innerMessage.put("role", "user");
		innerMessage.put("content", content);
		message.put("message", innerMessage);

		try {
			String json = objectMapper.writeValueAsString(message);
			logger.debug("Sending user message: {}", json);

			// Emit to outbound sink for async processing
			Sinks.EmitResult result = outboundSink.tryEmitNext(json);
			if (result.isFailure()) {
				throw new TransportException("Failed to queue user message: " + result);
			}
		}
		catch (IOException e) {
			throw new TransportException("Failed to serialize user message", e);
		}
	}

	/**
	 * Sends a control response back to the CLI via stdin.
	 * @param response the response to send
	 * @throws ClaudeSDKException if sending fails
	 */
	public void sendResponse(ControlResponse response) throws ClaudeSDKException {
		assertConnected();

		try {
			String json = objectMapper.writeValueAsString(response);
			logger.debug("Sending control response: {}", json);

			Sinks.EmitResult result = outboundSink.tryEmitNext(json);
			if (result.isFailure()) {
				throw new TransportException("Failed to queue control response: " + result);
			}
		}
		catch (IOException e) {
			throw new TransportException("Failed to serialize control response", e);
		}
	}

	/**
	 * Sends a raw message to the CLI via stdin.
	 * @param message the message JSON to send
	 * @throws ClaudeSDKException if sending fails
	 */
	public void sendMessage(String message) throws ClaudeSDKException {
		assertConnected();

		logger.debug("Sending message: {}", message);
		Sinks.EmitResult result = outboundSink.tryEmitNext(message);
		if (result.isFailure()) {
			throw new TransportException("Failed to queue message: " + result);
		}
	}

	private void assertConnected() {
		int currentState = state.get();
		if (currentState != STATE_CONNECTED) {
			if (currentState == STATE_CLOSED || currentState == STATE_CLOSING) {
				throw new SessionClosedException("Transport is closed");
			}
			throw new IllegalStateException("Transport not connected. State: " + getStateName());
		}
	}

	// ============================================================
	// Reactive API
	// ============================================================

	/**
	 * Returns a Flux of all inbound messages. This is the reactive API for message
	 * consumption.
	 */
	public Flux<ParsedMessage> receiveMessages() {
		return inboundSink.asFlux();
	}

	/**
	 * Returns a Mono that completes when the server info is received.
	 */
	public Mono<Map<String, Object>> getServerInfo() {
		return serverInfoSink.asMono();
	}

	// ============================================================
	// Iterator API (Critical - not in MCP/ACP)
	// ============================================================

	/**
	 * Returns an iterator over inbound messages. This enables non-reactive consumers to
	 * process messages using standard Iterator/Iterable patterns.
	 *
	 * <p>
	 * Usage:
	 * </p>
	 *
	 * <pre>{@code
	 * try (BidirectionalTransport transport = new BidirectionalTransport(...)) {
	 *     transport.startSession(...);
	 *     for (ParsedMessage message : transport.messageIterable()) {
	 *         handleMessage(message);
	 *     }
	 * }
	 * }</pre>
	 */
	public Iterator<ParsedMessage> messageIterator() {
		return inboundSink.asFlux().toIterable().iterator();
	}

	/**
	 * Returns an iterable over inbound messages for use with for-each loops.
	 */
	public Iterable<ParsedMessage> messageIterable() {
		return inboundSink.asFlux().toIterable();
	}

	// ============================================================
	// Status and Lifecycle
	// ============================================================

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
					throw TransportException.withExitCode("CLI process failed", exitCode);
				}
			}

			Throwable err = sessionError.get();
			if (err != null) {
				throw new TransportException("Session error", err);
			}

			return completed;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TransportException("Wait interrupted", e);
		}
	}

	/**
	 * Checks if the session is currently running.
	 */
	public boolean isRunning() {
		return state.get() == STATE_CONNECTED && process != null && process.isAlive();
	}

	/**
	 * Gets any error that occurred during the session.
	 */
	public Throwable getSessionError() {
		return sessionError.get();
	}

	/**
	 * Gets the session ID if assigned.
	 */
	public String getSessionId() {
		return sessionId.get();
	}

	/**
	 * Interrupts the current session.
	 */
	public void interrupt() {
		if (state.get() == STATE_CONNECTED) {
			transitionTo(STATE_CONNECTED, STATE_CLOSING);
		}
		if (process != null) {
			process.destroy();
		}
	}

	// ============================================================
	// Graceful Shutdown (from MCP SDK pattern)
	// ============================================================

	/**
	 * Initiates graceful shutdown. Returns a Mono that completes when shutdown is done.
	 */
	public Mono<Void> closeGracefully() {
		return Mono.fromRunnable(() -> {
			// Set isClosing first for immediate visibility to read loops (MCP pattern)
			isClosing = true;

			// State transition to CLOSING
			int currentState = state.get();
			if (currentState == STATE_CLOSED || currentState == STATE_CLOSING) {
				return;
			}
			state.set(STATE_CLOSING);
			logger.debug("Initiating graceful shutdown");
		}).then(Mono.defer(() -> {
			// Complete all sinks
			inboundSink.tryEmitComplete();
			outboundSink.tryEmitComplete();

			// Allow time for pending messages
			return Mono.delay(Duration.ofMillis(100));
		})).then(Mono.defer(() -> {
			// Dispose all subscriptions
			subscriptions.dispose();

			return Mono.empty();
		})).then(Mono.defer(() -> {
			// Close transport resources
			closeStreams();

			// Terminate process gracefully
			if (process != null) {
				process.destroy();
				return Mono.fromFuture(process.onExit()).then();
			}
			return Mono.empty();
		})).then(Mono.<Void>fromRunnable(() -> {
			// Dispose schedulers
			inboundScheduler.dispose();
			outboundScheduler.dispose();
			errorScheduler.dispose();

			state.set(STATE_CLOSED);
			logger.debug("BidirectionalTransport closed gracefully");
		})).subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public void close() {
		// Synchronous close for AutoCloseable compatibility
		int currentState = state.get();
		if (currentState == STATE_CLOSED) {
			return;
		}

		// Set isClosing first for immediate visibility to read loops (MCP pattern)
		isClosing = true;
		state.set(STATE_CLOSING);

		// Complete sinks
		inboundSink.tryEmitComplete();
		outboundSink.tryEmitComplete();

		// Dispose subscriptions
		subscriptions.dispose();

		// Close streams
		closeStreams();

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

		// Shutdown schedulers
		inboundScheduler.dispose();
		outboundScheduler.dispose();
		errorScheduler.dispose();

		state.set(STATE_CLOSED);
		logger.debug("BidirectionalTransport closed");
	}

	private void closeStreams() {
		closeQuietly(stdinWriter);
		closeQuietly(stdoutReader);
		closeQuietly(stderrReader);
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

	// ============================================================
	// Functional Interface
	// ============================================================

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
