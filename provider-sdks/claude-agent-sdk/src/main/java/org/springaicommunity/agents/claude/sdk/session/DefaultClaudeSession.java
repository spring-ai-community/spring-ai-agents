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

package org.springaicommunity.agents.claude.sdk.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.claude.sdk.exceptions.TransportException;
import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.hooks.HookCallback;
import org.springaicommunity.agents.claude.sdk.hooks.HookRegistry;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.streaming.BlockingMessageReceiver;
import org.springaicommunity.agents.claude.sdk.streaming.MessageReceiver;
import org.springaicommunity.agents.claude.sdk.streaming.MessageStreamIterator;
import org.springaicommunity.agents.claude.sdk.streaming.ResponseBoundedReceiver;
import org.springaicommunity.agents.claude.sdk.transport.BidirectionalTransport;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlRequest;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;
import org.springaicommunity.agents.claude.sdk.types.control.HookInput;
import org.springaicommunity.agents.claude.sdk.types.control.HookOutput;
import org.springaicommunity.agents.model.sandbox.Sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClaudeSession} providing persistent session support.
 *
 * <p>
 * This implementation maintains a persistent connection to the Claude CLI, allowing
 * multi-turn conversations where context is preserved across queries.
 * </p>
 *
 * <p>
 * Thread-safety: This class is thread-safe. Multiple threads can call query() and consume
 * messages concurrently, though typically one thread sends queries and another consumes
 * responses.
 * </p>
 *
 * @see ClaudeSession
 * @see BidirectionalTransport
 */
public class DefaultClaudeSession implements ClaudeSession {

	private static final Logger logger = LoggerFactory.getLogger(DefaultClaudeSession.class);

	private static final String DEFAULT_SESSION_ID = "default";

	private final Path workingDirectory;

	private final CLIOptions options;

	private final Duration timeout;

	private final Sandbox sandbox;

	private final String claudePath;

	private final HookRegistry hookRegistry;

	private final ObjectMapper objectMapper;

	// Session state
	private final AtomicBoolean connected = new AtomicBoolean(false);

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private final AtomicReference<Map<String, Object>> serverInfo = new AtomicReference<>(Collections.emptyMap());

	private final AtomicReference<String> currentSessionId = new AtomicReference<>(DEFAULT_SESSION_ID);

	// Runtime state tracking (P2 feature)
	private final AtomicReference<String> currentModel = new AtomicReference<>();

	private final AtomicReference<String> currentPermissionMode = new AtomicReference<>();

	// Transport and streaming
	private volatile BidirectionalTransport transport;

	private volatile MessageStreamIterator messageIterator;

	private volatile BlockingMessageReceiver blockingReceiver;

	// Control request handling
	private final AtomicInteger requestCounter = new AtomicInteger(0);

	private final ConcurrentHashMap<String, CountDownLatch> pendingRequests = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<String, Map<String, Object>> pendingResults = new ConcurrentHashMap<>();

	/**
	 * Creates a session with the specified working directory.
	 * @param workingDirectory the working directory for Claude CLI
	 */
	public DefaultClaudeSession(Path workingDirectory) {
		this(workingDirectory, CLIOptions.builder().build(), Duration.ofMinutes(10), null, null, null);
	}

	/**
	 * Creates a session with full configuration.
	 * @param workingDirectory the working directory for Claude CLI
	 * @param options CLI options
	 * @param timeout default operation timeout
	 * @param claudePath optional path to Claude CLI
	 * @param sandbox optional sandbox for process execution
	 * @param hookRegistry optional hook registry
	 */
	public DefaultClaudeSession(Path workingDirectory, CLIOptions options, Duration timeout, String claudePath,
			Sandbox sandbox, HookRegistry hookRegistry) {
		this.workingDirectory = workingDirectory;
		this.options = options != null ? options : CLIOptions.builder().build();
		this.timeout = timeout != null ? timeout : Duration.ofMinutes(10);
		this.claudePath = claudePath;
		this.sandbox = sandbox;
		this.hookRegistry = hookRegistry != null ? hookRegistry : new HookRegistry();
		this.objectMapper = new ObjectMapper();

		// Initialize runtime state from options
		if (this.options.getModel() != null) {
			this.currentModel.set(this.options.getModel());
		}
		if (this.options.getPermissionMode() != null) {
			this.currentPermissionMode.set(this.options.getPermissionMode().getValue());
		}
	}

	/**
	 * Builder for creating DefaultClaudeSession instances.
	 */
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void connect() throws ClaudeSDKException {
		connect(null);
	}

	@Override
	public void connect(String initialPrompt) throws ClaudeSDKException {
		if (closed.get()) {
			throw new TransportException("Session has been closed");
		}
		if (connected.get()) {
			throw new TransportException("Session is already connected");
		}

		try {
			// Create transport
			transport = new BidirectionalTransport(workingDirectory, timeout, claudePath, sandbox);

			// Create message receivers (both iterator and POC pattern)
			messageIterator = new MessageStreamIterator();
			blockingReceiver = new BlockingMessageReceiver();

			// Build effective prompt
			String effectivePrompt = initialPrompt != null ? initialPrompt : "Hello";

			// Start session with control request and response handling
			// Pass null for initial prompt - we'll send it after initialization
			transport.startSession(null, options, this::handleMessage, this::handleControlRequest,
					this::handleControlResponse);

			connected.set(true);

			// Send initialize request with hook configuration if hooks are registered
			if (hookRegistry.hasHooks()) {
				sendInitialize();
			}

			// Now send the initial prompt
			if (effectivePrompt != null) {
				transport.sendUserMessage(effectivePrompt, "default");
			}

			logger.info("Session connected with prompt: {}",
					effectivePrompt.substring(0, Math.min(50, effectivePrompt.length())));
		}
		catch (Exception e) {
			cleanup();
			throw new TransportException("Failed to connect session", e);
		}
	}

	/**
	 * Sends the initialize control request with hook configuration.
	 */
	private void sendInitialize() throws ClaudeSDKException {
		Map<String, List<ControlRequest.HookMatcherConfig>> hookConfig = hookRegistry.buildHookConfig();

		if (hookConfig.isEmpty()) {
			logger.debug("No hooks to initialize");
			return;
		}

		Map<String, Object> request = new LinkedHashMap<>();
		request.put("subtype", "initialize");
		request.put("hooks", hookConfig);

		logger.debug("Sending initialize with {} hook event types", hookConfig.size());
		sendControlRequest(request);
		logger.info("Hook configuration sent to CLI: {} event types", hookConfig.size());
	}

	@Override
	public void query(String prompt) throws ClaudeSDKException {
		query(prompt, currentSessionId.get());
	}

	@Override
	public void query(String prompt, String sessionId) throws ClaudeSDKException {
		ensureConnected();

		try {
			// Format user message per Python SDK protocol
			Map<String, Object> message = new LinkedHashMap<>();
			message.put("type", "user");

			Map<String, String> innerMessage = new LinkedHashMap<>();
			innerMessage.put("role", "user");
			innerMessage.put("content", prompt);
			message.put("message", innerMessage);

			message.put("parent_tool_use_id", null);
			message.put("session_id", sessionId);

			String json = objectMapper.writeValueAsString(message);
			transport.sendMessage(json);

			currentSessionId.set(sessionId);
			logger.debug("Sent query in session {}: {}", sessionId, prompt.substring(0, Math.min(50, prompt.length())));
		}
		catch (Exception e) {
			throw new TransportException("Failed to send query", e);
		}
	}

	@Override
	public Iterator<ParsedMessage> receiveMessages() {
		ensureConnected();
		return messageIterator;
	}

	@Override
	public Iterator<ParsedMessage> receiveResponse() {
		ensureConnected();
		return new ResponseBoundedIterator(messageIterator);
	}

	@Override
	public MessageReceiver messageReceiver() {
		ensureConnected();
		return blockingReceiver;
	}

	@Override
	public MessageReceiver responseReceiver() {
		ensureConnected();
		return new ResponseBoundedReceiver(blockingReceiver);
	}

	@Override
	public void interrupt() throws ClaudeSDKException {
		ensureConnected();
		sendControlRequest(Map.of("subtype", "interrupt"));
	}

	@Override
	public void setPermissionMode(String mode) throws ClaudeSDKException {
		ensureConnected();
		sendControlRequest(Map.of("subtype", "set_permission_mode", "mode", mode));
		// Update local state after successful change
		currentPermissionMode.set(mode);
	}

	@Override
	public void setModel(String model) throws ClaudeSDKException {
		ensureConnected();
		Map<String, Object> request = new LinkedHashMap<>();
		request.put("subtype", "set_model");
		request.put("model", model);
		sendControlRequest(request);
		// Update local state after successful change
		currentModel.set(model);
	}

	/**
	 * Gets the current model being used by this session. This reflects any runtime
	 * changes made via {@link #setModel(String)}.
	 * @return the current model ID, or null if not explicitly set
	 */
	public String getCurrentModel() {
		return currentModel.get();
	}

	/**
	 * Gets the current permission mode for this session. This reflects any runtime
	 * changes made via {@link #setPermissionMode(String)}.
	 * @return the current permission mode, or null if not explicitly set
	 */
	public String getCurrentPermissionMode() {
		return currentPermissionMode.get();
	}

	@Override
	public Map<String, Object> getServerInfo() {
		return serverInfo.get();
	}

	@Override
	public boolean isConnected() {
		return connected.get() && !closed.get() && transport != null && transport.isRunning();
	}

	@Override
	public void disconnect() {
		close();
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			connected.set(false);
			cleanup();
			logger.info("Session closed");
		}
	}

	/**
	 * Registers a hook callback for a specific event and tool pattern.
	 * @param event the hook event type
	 * @param toolPattern regex pattern for tool names, or null for all tools
	 * @param callback the callback to execute
	 * @return this session for chaining
	 */
	public DefaultClaudeSession registerHook(org.springaicommunity.agents.claude.sdk.types.control.HookEvent event,
			String toolPattern, HookCallback callback) {
		hookRegistry.register(event, toolPattern, callback);
		return this;
	}

	/**
	 * Gets the current session ID.
	 */
	public String getCurrentSessionId() {
		return currentSessionId.get();
	}

	private void handleMessage(ParsedMessage message) {
		// Forward regular messages to both receivers
		if (message.isRegularMessage()) {
			messageIterator.offer(message);
			blockingReceiver.offer(message);
		}
	}

	private ControlResponse handleControlRequest(ControlRequest request) {
		String requestId = request.requestId();
		ControlRequest.ControlRequestPayload payload = request.request();

		logger.debug("Handling control request: type={}, requestId={}", payload != null ? payload.subtype() : "null",
				requestId);

		try {
			if (payload instanceof ControlRequest.HookCallbackRequest hookCallback) {
				return handleHookCallback(requestId, hookCallback);
			}
			else if (payload instanceof ControlRequest.CanUseToolRequest canUseTool) {
				return handleCanUseTool(requestId, canUseTool);
			}
			else if (payload instanceof ControlRequest.InitializeRequest init) {
				// Store server info from initialization
				serverInfo.set(Map.of("hooks", init.hooks() != null ? init.hooks() : Collections.emptyMap()));
				return ControlResponse.success(requestId, Map.of("status", "ok"));
			}
			else {
				// Unknown request type - acknowledge
				return ControlResponse.success(requestId, Map.of());
			}
		}
		catch (Exception e) {
			logger.error("Error handling control request", e);
			return ControlResponse.error(requestId, e.getMessage());
		}
	}

	private ControlResponse handleHookCallback(String requestId, ControlRequest.HookCallbackRequest hookCallback) {
		try {
			String callbackId = hookCallback.callbackId();
			Map<String, Object> inputMap = hookCallback.input();

			// Parse input to typed HookInput
			HookInput input = objectMapper.convertValue(inputMap, HookInput.class);

			// Execute hook
			HookOutput output = hookRegistry.executeHook(callbackId, input);

			// Build response
			Map<String, Object> responsePayload = new LinkedHashMap<>();
			responsePayload.put("continue", output.continueExecution());
			if (output.decision() != null) {
				responsePayload.put("decision", output.decision());
			}
			if (output.reason() != null) {
				responsePayload.put("reason", output.reason());
			}
			if (output.hookSpecificOutput() != null) {
				HookOutput.HookSpecificOutput specific = output.hookSpecificOutput();
				if (specific.permissionDecision() != null) {
					responsePayload.put("permission_decision", specific.permissionDecision());
				}
				if (specific.permissionDecisionReason() != null) {
					responsePayload.put("permission_decision_reason", specific.permissionDecisionReason());
				}
				if (specific.updatedInput() != null) {
					responsePayload.put("updated_input", specific.updatedInput());
				}
			}

			return ControlResponse.success(requestId, responsePayload);
		}
		catch (Exception e) {
			logger.error("Error executing hook callback", e);
			return ControlResponse.error(requestId, e.getMessage());
		}
	}

	private ControlResponse handleCanUseTool(String requestId, ControlRequest.CanUseToolRequest canUseTool) {
		// Default: allow all tools
		return ControlResponse.success(requestId, Map.of("behavior", "allow"));
	}

	/**
	 * Handles control responses from the CLI for our outgoing control requests
	 * (interrupt, set_model, set_permission_mode).
	 */
	private void handleControlResponse(ControlResponse response) {
		if (response.response() == null) {
			logger.warn("Received control response with null payload");
			return;
		}

		String requestId = response.response().requestId();
		if (requestId == null) {
			logger.warn("Received control response without request_id");
			return;
		}

		logger.debug("Handling control response: requestId={}, subtype={}", requestId, response.response().subtype());

		// Check if this is a response to one of our pending requests
		CountDownLatch latch = pendingRequests.get(requestId);
		if (latch != null) {
			// Extract response payload
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("subtype", response.response().subtype());

			if (response.response() instanceof ControlResponse.SuccessPayload success) {
				if (success.response() instanceof Map<?, ?> responseMap) {
					@SuppressWarnings("unchecked")
					Map<String, Object> typedMap = (Map<String, Object>) responseMap;
					payload.putAll(typedMap);
				}
			}
			else if (response.response() instanceof ControlResponse.ErrorPayload error) {
				payload.put("error", error.error());
			}

			// Store result and signal completion
			pendingResults.put(requestId, payload);
			latch.countDown();
			logger.debug("Control response stored for requestId={}", requestId);
		}
		else {
			logger.debug("No pending request found for control response: requestId={}", requestId);
		}
	}

	private void sendControlRequest(Map<String, Object> request) throws ClaudeSDKException {
		ensureConnected();

		try {
			// Generate request ID
			int counter = requestCounter.incrementAndGet();
			String requestId = "req_" + counter + "_" + UUID.randomUUID().toString().substring(0, 8);

			// Create latch for response
			CountDownLatch latch = new CountDownLatch(1);
			pendingRequests.put(requestId, latch);

			// Build control request
			Map<String, Object> controlRequest = new LinkedHashMap<>();
			controlRequest.put("type", "control_request");
			controlRequest.put("request_id", requestId);
			controlRequest.put("request", request);

			String json = objectMapper.writeValueAsString(controlRequest);
			transport.sendMessage(json);

			// Wait for response (with timeout)
			boolean received = latch.await(timeout.toSeconds(), TimeUnit.SECONDS);
			pendingRequests.remove(requestId);

			if (!received) {
				pendingResults.remove(requestId);
				throw new ClaudeSDKException("Control request timed out: " + request.get("subtype"));
			}

			Map<String, Object> result = pendingResults.remove(requestId);
			if (result != null && result.containsKey("error")) {
				throw new ClaudeSDKException("Control request failed: " + result.get("error"));
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ClaudeSDKException("Control request interrupted", e);
		}
		catch (ClaudeSDKException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ClaudeSDKException("Failed to send control request", e);
		}
	}

	private void ensureConnected() {
		if (!connected.get()) {
			throw new IllegalStateException("Session is not connected. Call connect() first.");
		}
		if (closed.get()) {
			throw new IllegalStateException("Session has been closed.");
		}
	}

	private void cleanup() {
		if (messageIterator != null) {
			messageIterator.complete();
			messageIterator.close();
		}
		if (blockingReceiver != null) {
			blockingReceiver.complete();
			blockingReceiver.close();
		}
		if (transport != null) {
			transport.close();
		}
		pendingRequests.clear();
		pendingResults.clear();
	}

	/**
	 * Iterator that stops after receiving a ResultMessage.
	 *
	 * <p>
	 * Note: This iterator is designed to be used with the standard iteration pattern
	 * where {@link #hasNext()} is called before each {@link #next()} call. The
	 * {@code next()} method does NOT defensively call {@code hasNext()} to avoid race
	 * conditions with the underlying timeout-based polling iterator.
	 * </p>
	 */
	private static class ResponseBoundedIterator implements Iterator<ParsedMessage> {

		private final Iterator<ParsedMessage> delegate;

		private ParsedMessage next;

		private boolean resultReceived = false;

		ResponseBoundedIterator(Iterator<ParsedMessage> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean hasNext() {
			if (resultReceived) {
				return false;
			}
			if (next != null) {
				return true;
			}
			if (delegate.hasNext()) {
				next = delegate.next();
				// Check if this is a result message
				if (next.isRegularMessage()) {
					Message msg = next.asMessage();
					if (msg instanceof ResultMessage) {
						resultReceived = true;
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public ParsedMessage next() {
			// Do NOT call hasNext() here - this causes race conditions with
			// the underlying timeout-based polling iterator. Per Iterator contract,
			// caller must call hasNext() before next().
			if (next == null) {
				throw new NoSuchElementException("No element available. Did you call hasNext() first?");
			}
			ParsedMessage result = next;
			next = null;
			return result;
		}

	}

	/**
	 * Builder for DefaultClaudeSession.
	 */
	public static class Builder {

		private Path workingDirectory;

		private CLIOptions options;

		private Duration timeout = Duration.ofMinutes(10);

		private String claudePath;

		private Sandbox sandbox;

		private HookRegistry hookRegistry;

		public Builder workingDirectory(Path workingDirectory) {
			this.workingDirectory = workingDirectory;
			return this;
		}

		public Builder options(CLIOptions options) {
			this.options = options;
			return this;
		}

		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder claudePath(String claudePath) {
			this.claudePath = claudePath;
			return this;
		}

		public Builder sandbox(Sandbox sandbox) {
			this.sandbox = sandbox;
			return this;
		}

		public Builder hookRegistry(HookRegistry hookRegistry) {
			this.hookRegistry = hookRegistry;
			return this;
		}

		public DefaultClaudeSession build() {
			if (workingDirectory == null) {
				throw new IllegalArgumentException("workingDirectory is required");
			}
			return new DefaultClaudeSession(workingDirectory, options, timeout, claudePath, sandbox, hookRegistry);
		}

	}

}
