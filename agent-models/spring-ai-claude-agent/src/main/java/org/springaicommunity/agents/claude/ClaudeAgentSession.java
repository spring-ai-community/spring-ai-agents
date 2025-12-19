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

package org.springaicommunity.agents.claude;

import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.hooks.HookCallback;
import org.springaicommunity.agents.claude.sdk.hooks.HookRegistry;
import org.springaicommunity.agents.claude.sdk.types.control.HookEvent;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.session.ClaudeSession;
import org.springaicommunity.agents.claude.sdk.session.DefaultClaudeSession;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentGenerationMetadata;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * High-level session API for multi-turn conversations with Claude.
 *
 * <p>
 * A session maintains conversation context across multiple queries, allowing Claude to
 * remember previous messages and build on prior context.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * try (ClaudeAgentSession session = ClaudeAgentModel.builder()
 *         .workingDirectory(projectDir)
 *         .build()
 *         .createSession()) {
 *
 *     // First query
 *     session.query("Analyze this codebase");
 *     for (AgentResponse response : session.receiveResponse()) {
 *         System.out.println(response.getTextOutput());
 *     }
 *
 *     // Follow-up (Claude remembers the first query)
 *     session.query("Now implement the fix we discussed");
 *     for (AgentResponse response : session.receiveResponse()) {
 *         System.out.println(response.getTextOutput());
 *     }
 *
 *     // Change permissions mid-session
 *     session.setPermissionMode(PermissionMode.ACCEPT_EDITS);
 *
 *     // Continue with more queries...
 * }
 * }</pre>
 *
 * @author Spring AI Community
 * @since 0.1.0
 * @see ClaudeAgentModel#createSession()
 */
public class ClaudeAgentSession implements AutoCloseable {

	private final DefaultClaudeSession delegate;

	private final HookRegistry hookRegistry;

	/**
	 * Creates a session with the given delegate and hook registry.
	 * @param delegate the underlying session implementation
	 * @param hookRegistry the hook registry (may be shared with parent model)
	 */
	ClaudeAgentSession(DefaultClaudeSession delegate, HookRegistry hookRegistry) {
		this.delegate = delegate;
		this.hookRegistry = hookRegistry;
	}

	/**
	 * Creates a new session builder.
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Connects to Claude CLI without an initial prompt.
	 * @throws org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException if
	 * connection fails
	 */
	public void connect() {
		delegate.connect();
	}

	/**
	 * Connects to Claude CLI with an initial prompt.
	 * @param initialPrompt the first prompt to send
	 * @throws org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException if
	 * connection fails
	 */
	public void connect(String initialPrompt) {
		delegate.connect(initialPrompt);
	}

	/**
	 * Sends a follow-up query in the existing session context. Claude will remember
	 * previous messages in this session.
	 * @param prompt the prompt to send
	 * @throws org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException if
	 * sending fails
	 */
	public void query(String prompt) {
		delegate.query(prompt);
	}

	/**
	 * Sends a follow-up query with a specific session ID.
	 * @param prompt the prompt to send
	 * @param sessionId the session ID to use
	 * @throws org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException if
	 * sending fails
	 */
	public void query(String prompt, String sessionId) {
		delegate.query(prompt, sessionId);
	}

	/**
	 * Returns an iterator that yields AgentResponse objects until a result is received.
	 * This is the primary way to consume responses after calling {@link #query(String)}.
	 * @return iterator over agent responses
	 */
	public Iterable<AgentResponse> receiveResponse() {
		return () -> new AgentResponseIterator(delegate.receiveResponse(), true);
	}

	/**
	 * Returns an iterator that yields all AgentResponse objects indefinitely. Use this
	 * when you want to receive all messages in the session.
	 * @return iterator over agent responses
	 */
	public Iterable<AgentResponse> receiveMessages() {
		return () -> new AgentResponseIterator(delegate.receiveMessages(), false);
	}

	/**
	 * Returns a Flux of AgentResponse objects until a result is received. This is the
	 * reactive variant of {@link #receiveResponse()}.
	 * @return flux of agent responses
	 */
	public Flux<AgentResponse> receiveResponseFlux() {
		Sinks.Many<AgentResponse> sink = Sinks.many().multicast().onBackpressureBuffer();

		Thread.startVirtualThread(() -> {
			try {
				for (AgentResponse response : receiveResponse()) {
                    if (response == null) {
                        sink.tryEmitComplete();
                        break;
                    }
                    sink.tryEmitNext(response);
                }
			}
			catch (Exception e) {
				sink.tryEmitError(e);
			}
		});

		return sink.asFlux();
	}

	/**
	 * Interrupts the current operation.
	 * @throws org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException if
	 * interrupt fails
	 */
	public void interrupt() {
		delegate.interrupt();
	}

	/**
	 * Changes the permission mode mid-session.
	 * @param mode the new permission mode
	 * @throws org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException if
	 * setting mode fails
	 */
	public void setPermissionMode(PermissionMode mode) {
		delegate.setPermissionMode(mode.getValue());
	}

	/**
	 * Changes the permission mode mid-session using a string value.
	 * @param mode the new permission mode as string
	 * @throws org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException if
	 * setting mode fails
	 */
	public void setPermissionMode(String mode) {
		delegate.setPermissionMode(mode);
	}

	/**
	 * Changes the model mid-session.
	 * @param model the new model name (e.g., "claude-sonnet-4-20250514")
	 * @throws org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException if
	 * setting model fails
	 */
	public void setModel(String model) {
		delegate.setModel(model);
	}

	/**
	 * Gets server initialization info.
	 * @return map of server information
	 */
	public Map<String, Object> getServerInfo() {
		return delegate.getServerInfo();
	}

	/**
	 * Checks if the session is connected.
	 * @return true if connected and ready for queries
	 */
	public boolean isConnected() {
		return delegate.isConnected();
	}

	/**
	 * Disconnects the session.
	 */
	public void disconnect() {
		delegate.disconnect();
	}

	/**
	 * Registers a hook callback for a specific event and tool pattern.
	 * @param event the hook event type
	 * @param toolPattern regex pattern for tool names, or null for all tools
	 * @param callback the callback to execute
	 * @return this session for chaining
	 */
	public ClaudeAgentSession registerHook(HookEvent event, String toolPattern, HookCallback callback) {
		hookRegistry.register(event, toolPattern, callback);
		return this;
	}

	/**
	 * Registers a pre-tool-use hook for specific tools.
	 * @param toolPattern regex pattern for tool names
	 * @param callback the hook callback
	 * @return this session for chaining
	 */
	public ClaudeAgentSession registerPreToolUse(String toolPattern, HookCallback callback) {
		hookRegistry.registerPreToolUse(toolPattern, callback);
		return this;
	}

	/**
	 * Registers a post-tool-use hook for specific tools.
	 * @param toolPattern regex pattern for tool names
	 * @param callback the hook callback
	 * @return this session for chaining
	 */
	public ClaudeAgentSession registerPostToolUse(String toolPattern, HookCallback callback) {
		hookRegistry.registerPostToolUse(toolPattern, callback);
		return this;
	}

	@Override
	public void close() {
		delegate.close();
	}

	/**
	 * Iterator that converts ParsedMessage to AgentResponse.
	 */
	private static class AgentResponseIterator implements Iterator<AgentResponse> {

		private final Iterator<ParsedMessage> delegate;

		private final boolean stopAtResult;

		private AgentResponse next;

		private boolean resultReceived = false;

		AgentResponseIterator(Iterator<ParsedMessage> delegate, boolean stopAtResult) {
			this.delegate = delegate;
			this.stopAtResult = stopAtResult;
		}

		@Override
		public boolean hasNext() {
			if (stopAtResult && resultReceived) {
				return false;
			}
			if (next != null) {
				return true;
			}

			while (delegate.hasNext()) {
				ParsedMessage parsed = delegate.next();
				if (parsed.isRegularMessage()) {
					Message message = parsed.asMessage();
					AgentResponse response = convertToAgentResponse(message);
					if (response != null) {
						next = response;
						if (message instanceof ResultMessage) {
							resultReceived = true;
						}
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public AgentResponse next() {
			if (!hasNext()) {
                return null;
			}
			AgentResponse result = next;
			next = null;
			return result;
		}

		private AgentResponse convertToAgentResponse(Message message) {
			if (message instanceof AssistantMessage assistantMessage) {
				String text = assistantMessage.getTextContent().orElse("");
				if (!text.isEmpty()) {
					AgentGenerationMetadata metadata = new AgentGenerationMetadata("STREAMING", Map.of());
					List<AgentGeneration> generations = List.of(new AgentGeneration(text, metadata));
					return new AgentResponse(generations, new AgentResponseMetadata());
				}
			}
			else if (message instanceof ResultMessage resultMessage) {
				String text = resultMessage.result() != null ? resultMessage.result() : "";
				String finishReason = resultMessage.isError() ? "ERROR" : "SUCCESS";
				AgentGenerationMetadata metadata = new AgentGenerationMetadata(finishReason, Map.of());
				List<AgentGeneration> generations = List.of(new AgentGeneration(text, metadata));
				return new AgentResponse(generations, new AgentResponseMetadata());
			}
			return null;
		}

	}

	/**
	 * Builder for ClaudeAgentSession.
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

		public ClaudeAgentSession build() {
			if (workingDirectory == null) {
				workingDirectory = Path.of(System.getProperty("user.dir"));
			}
			HookRegistry registry = hookRegistry != null ? hookRegistry : new HookRegistry();
			DefaultClaudeSession delegate = DefaultClaudeSession.builder()
				.workingDirectory(workingDirectory)
				.options(options)
				.timeout(timeout)
				.claudePath(claudePath)
				.sandbox(sandbox)
				.hookRegistry(registry)
				.build();
			return new ClaudeAgentSession(delegate, registry);
		}

	}

}
