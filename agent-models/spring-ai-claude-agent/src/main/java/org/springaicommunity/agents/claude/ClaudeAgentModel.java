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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.claude.agent.sdk.ClaudeClient;
import org.springaicommunity.claude.agent.sdk.ClaudeSyncClient;
import org.springaicommunity.claude.agent.sdk.hooks.HookCallback;
import org.springaicommunity.claude.agent.sdk.hooks.HookRegistry;
import org.springaicommunity.claude.agent.sdk.parsing.ParsedMessage;
import org.springaicommunity.claude.agent.sdk.transport.CLIOptions;
import org.springaicommunity.claude.agent.sdk.types.AssistantMessage;
import org.springaicommunity.claude.agent.sdk.types.Message;
import org.springaicommunity.claude.agent.sdk.types.ResultMessage;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentGenerationMetadata;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.IterableAgentModel;
import org.springaicommunity.agents.model.StreamingAgentModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Claude Code CLI agent model implementing all three programming models:
 * blocking/imperative, reactive/streaming, and iterator-based.
 *
 * <p>
 * This is the primary entry point for Claude Code CLI integration. It supports:
 * </p>
 * <ul>
 * <li>{@link AgentModel#call} - Blocking execution</li>
 * <li>{@link StreamingAgentModel#stream} - Reactive Flux-based streaming</li>
 * <li>{@link IterableAgentModel#iterate} - Iterator-based consumption</li>
 * </ul>
 *
 * <p>
 * Hooks can be registered to intercept and modify tool executions:
 * </p>
 * <pre>{@code
 * var model = ClaudeAgentModel.builder()
 *     .workingDirectory(Paths.get("/my/project"))
 *     .timeout(Duration.ofMinutes(5))
 *     .build();
 *
 * // Register a pre-tool-use hook to block dangerous commands
 * model.registerPreToolUse("Bash", input -> {
 *     var preToolUse = (HookInput.PreToolUseInput) input;
 *     String cmd = preToolUse.getArgument("command", String.class).orElse("");
 *     if (cmd.contains("rm -rf")) {
 *         return HookOutput.block("Dangerous command blocked");
 *     }
 *     return HookOutput.allow();
 * });
 *
 * // Use any programming model
 * AgentResponse response = model.call(request);           // Blocking
 * Flux<AgentResponse> flux = model.stream(request);       // Reactive
 * Iterator<AgentResponse> iter = model.iterate(request);  // Iterator
 * }</pre>
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class ClaudeAgentModel implements AgentModel, StreamingAgentModel, IterableAgentModel, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeAgentModel.class);

	/**
	 * Default executor using cached thread pool with daemon threads. Java 21 users can
	 * provide {@code Executors.newVirtualThreadPerTaskExecutor()} via the builder.
	 */
	private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
		private final AtomicInteger counter = new AtomicInteger(0);

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "claude-agent-async-" + counter.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});

	private final Path workingDirectory;

	private final Duration timeout;

	private final String claudePath;

	private final HookRegistry hookRegistry;

	private final ClaudeAgentOptions defaultOptions;

	private final Executor asyncExecutor;

	private ClaudeAgentModel(Builder builder) {
		this.workingDirectory = builder.workingDirectory;
		this.timeout = builder.timeout;
		this.claudePath = builder.claudePath;
		this.hookRegistry = builder.hookRegistry != null ? builder.hookRegistry : new HookRegistry();
		this.defaultOptions = builder.defaultOptions != null ? builder.defaultOptions : new ClaudeAgentOptions();
		this.asyncExecutor = builder.asyncExecutor != null ? builder.asyncExecutor : DEFAULT_EXECUTOR;
	}

	/**
	 * Creates a new builder for ClaudeAgentModel.
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	// ========== Hook Registration API ==========

	/**
	 * Registers a pre-tool-use hook for specific tools.
	 * @param toolPattern regex pattern for tool names
	 * @param callback the hook callback
	 * @return the generated hook ID
	 */
	public String registerPreToolUse(String toolPattern, HookCallback callback) {
		return hookRegistry.registerPreToolUse(toolPattern, callback);
	}

	/**
	 * Registers a pre-tool-use hook for all tools.
	 * @param callback the hook callback
	 * @return the generated hook ID
	 */
	public String registerPreToolUse(HookCallback callback) {
		return hookRegistry.registerPreToolUse(callback);
	}

	/**
	 * Registers a post-tool-use hook for specific tools.
	 * @param toolPattern regex pattern for tool names
	 * @param callback the hook callback
	 * @return the generated hook ID
	 */
	public String registerPostToolUse(String toolPattern, HookCallback callback) {
		return hookRegistry.registerPostToolUse(toolPattern, callback);
	}

	/**
	 * Registers a post-tool-use hook for all tools.
	 * @param callback the hook callback
	 * @return the generated hook ID
	 */
	public String registerPostToolUse(HookCallback callback) {
		return hookRegistry.registerPostToolUse(callback);
	}

	/**
	 * Registers a user-prompt-submit hook.
	 * @param callback the hook callback
	 * @return the generated hook ID
	 */
	public String registerUserPromptSubmit(HookCallback callback) {
		return hookRegistry.registerUserPromptSubmit(callback);
	}

	/**
	 * Registers a stop hook.
	 * @param callback the hook callback
	 * @return the generated hook ID
	 */
	public String registerStop(HookCallback callback) {
		return hookRegistry.registerStop(callback);
	}

	/**
	 * Unregisters a hook by ID.
	 * @param hookId the hook ID to remove
	 * @return true if removed, false if not found
	 */
	public boolean unregisterHook(String hookId) {
		return hookRegistry.unregister(hookId);
	}

	/**
	 * Gets the hook registry for advanced configuration.
	 * @return the hook registry
	 */
	public HookRegistry getHookRegistry() {
		return hookRegistry;
	}

	// ========== AgentModel (Blocking) ==========

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		logger.info("Executing blocking call for goal: {}", request.goal());

		Instant startTime = Instant.now();
		StringBuilder fullText = new StringBuilder();

		Path effectiveWorkingDir = request.workingDirectory() != null ? request.workingDirectory() : workingDirectory;
		CLIOptions options = buildCLIOptions(request);

		try (ClaudeSyncClient client = ClaudeClient.sync(options)
			.workingDirectory(effectiveWorkingDir)
			.timeout(timeout)
			.claudePath(claudePath)
			.hookRegistry(hookRegistry)
			.build()) {

			String prompt = formatPrompt(request);
			client.connect(prompt);

			Iterator<ParsedMessage> response = client.receiveResponse();
			while (response.hasNext()) {
				ParsedMessage parsed = response.next();
				if (parsed.isRegularMessage()) {
					Message message = parsed.asMessage();
					if (message instanceof AssistantMessage assistantMessage) {
						assistantMessage.getTextContent().ifPresent(fullText::append);
					}
				}
			}

			Duration duration = Duration.between(startTime, Instant.now());
			AgentGenerationMetadata generationMetadata = new AgentGenerationMetadata("SUCCESS", Map.of());
			List<AgentGeneration> generations = List.of(new AgentGeneration(fullText.toString(), generationMetadata));

			AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
				.model(getEffectiveModel())
				.duration(duration)
				.build();

			return new AgentResponse(generations, responseMetadata);

		}
		catch (Exception e) {
			logger.error("Call failed", e);
			Duration duration = Duration.between(startTime, Instant.now());
			return createErrorResponse(e.getMessage(), duration);
		}
	}

	// ========== StreamingAgentModel (Reactive) ==========

	@Override
	public Flux<AgentResponse> stream(AgentTaskRequest request) {
		Sinks.Many<AgentResponse> sink = Sinks.many().multicast().onBackpressureBuffer();

		asyncExecutor.execute(() -> {
			try {
				streamInternal(request, sink);
			}
			catch (Exception e) {
				logger.error("Streaming failed", e);
				sink.tryEmitError(e);
			}
		});

		return sink.asFlux();
	}

	// ========== IterableAgentModel (Iterator) ==========

	@Override
	public Iterator<AgentResponse> iterate(AgentTaskRequest request) {
		Path effectiveWorkingDir = request.workingDirectory() != null ? request.workingDirectory() : workingDirectory;
		CLIOptions options = buildCLIOptions(request);

		ClaudeSyncClient client = ClaudeClient.sync(options)
			.workingDirectory(effectiveWorkingDir)
			.timeout(timeout)
			.claudePath(claudePath)
			.hookRegistry(hookRegistry)
			.build();

		String prompt = formatPrompt(request);
		client.connect(prompt);
		Iterator<ParsedMessage> messageIterator = client.receiveResponse();

		return new Iterator<>() {
			private AgentResponse next = null;

			private boolean closed = false;

			@Override
			public boolean hasNext() {
				if (closed) {
					return false;
				}
				if (next != null) {
					return true;
				}
				while (messageIterator.hasNext()) {
					ParsedMessage parsed = messageIterator.next();
					if (parsed.isRegularMessage()) {
						AgentResponse response = convertMessageToResponse(parsed.asMessage());
						if (response != null) {
							next = response;
							return true;
						}
					}
				}
				// Close the client when iteration is complete
				if (!closed) {
					closed = true;
					client.close();
				}
				return false;
			}

			@Override
			public AgentResponse next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				AgentResponse result = next;
				next = null;
				return result;
			}
		};
	}

	// ========== Availability Check ==========

	@Override
	public boolean isAvailable() {
		try {
			// Try to create a client to verify CLI is available
			ClaudeSyncClient client = ClaudeClient.sync()
				.workingDirectory(workingDirectory != null ? workingDirectory : Path.of(System.getProperty("user.dir")))
				.timeout(Duration.ofSeconds(5))
				.claudePath(claudePath)
				.build();
			client.close();
			return true;
		}
		catch (Exception e) {
			logger.debug("Claude CLI not available: {}", e.getMessage());
			return false;
		}
	}

	// ========== AutoCloseable ==========

	@Override
	public void close() {
		hookRegistry.clear();
	}

	// ========== Internal Implementation ==========

	private void streamInternal(AgentTaskRequest request, Sinks.Many<AgentResponse> sink) {
		Path effectiveWorkingDir = request.workingDirectory() != null ? request.workingDirectory() : workingDirectory;
		CLIOptions options = buildCLIOptions(request);

		try (ClaudeSyncClient client = ClaudeClient.sync(options)
			.workingDirectory(effectiveWorkingDir)
			.timeout(timeout)
			.claudePath(claudePath)
			.hookRegistry(hookRegistry)
			.build()) {

			String prompt = formatPrompt(request);
			client.connect(prompt);

			Iterator<ParsedMessage> response = client.receiveResponse();
			while (response.hasNext()) {
				ParsedMessage parsed = response.next();
				if (parsed.isRegularMessage()) {
					AgentResponse agentResponse = convertMessageToResponse(parsed.asMessage());
					if (agentResponse != null) {
						sink.tryEmitNext(agentResponse);
					}
				}
			}

			sink.tryEmitComplete();

		}
		catch (Exception e) {
			sink.tryEmitError(e);
		}
	}

	private AgentResponse convertMessageToResponse(Message message) {
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

	private CLIOptions buildCLIOptions(AgentTaskRequest request) {
		ClaudeAgentOptions options = getEffectiveOptions(request);
		CLIOptions.Builder builder = CLIOptions.builder();

		if (options.getTimeout() != null) {
			builder.timeout(options.getTimeout());
		}
		else if (timeout != null) {
			builder.timeout(timeout);
		}

		if (options.getModel() != null) {
			builder.model(options.getModel());
		}

		if (options.isYolo()) {
			builder.permissionMode(
					org.springaicommunity.claude.agent.sdk.config.PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS);
		}

		// Extended thinking
		if (options.getMaxThinkingTokens() != null) {
			builder.maxThinkingTokens(options.getMaxThinkingTokens());
		}

		// Max tokens
		if (options.getMaxTokens() != null) {
			builder.maxTokens(options.getMaxTokens());
		}

		// System prompt
		if (options.getSystemPrompt() != null) {
			SystemPrompt sp = options.getSystemPrompt();
			String promptText;
			if (sp instanceof SystemPrompt.StringPrompt stringPrompt) {
				promptText = stringPrompt.prompt();
			}
			else if (sp instanceof SystemPrompt.PresetPrompt presetPrompt) {
				promptText = presetPrompt.preset() + (presetPrompt.append() != null ? " " + presetPrompt.append() : "");
			}
			else {
				promptText = null;
			}
			if (promptText != null) {
				builder.systemPrompt(promptText);
			}
		}

		// Tool filtering
		if (options.getAllowedTools() != null && !options.getAllowedTools().isEmpty()) {
			builder.allowedTools(options.getAllowedTools());
		}
		if (options.getDisallowedTools() != null && !options.getDisallowedTools().isEmpty()) {
			builder.disallowedTools(options.getDisallowedTools());
		}

		// Structured output
		if (options.getJsonSchema() != null && !options.getJsonSchema().isEmpty()) {
			builder.jsonSchema(options.getJsonSchema());
		}

		// MCP servers
		if (options.getMcpServers() != null && !options.getMcpServers().isEmpty()) {
			builder.mcpServers(options.getMcpServers());
		}

		// Budget control
		if (options.getMaxTurns() != null) {
			builder.maxTurns(options.getMaxTurns());
		}
		if (options.getMaxBudgetUsd() != null) {
			builder.maxBudgetUsd(options.getMaxBudgetUsd());
		}

		// Fallback model
		if (options.getFallbackModel() != null && !options.getFallbackModel().isEmpty()) {
			builder.fallbackModel(options.getFallbackModel());
		}

		// Append system prompt (uses preset mode)
		if (options.getAppendSystemPrompt() != null && !options.getAppendSystemPrompt().isEmpty()) {
			builder.appendSystemPrompt(options.getAppendSystemPrompt());
		}

		return builder.build();
	}

	private ClaudeAgentOptions getEffectiveOptions(AgentTaskRequest request) {
		if (request.options() instanceof ClaudeAgentOptions requestOptions) {
			return requestOptions;
		}
		return defaultOptions;
	}

	private String getEffectiveModel() {
		return defaultOptions.getModel() != null ? defaultOptions.getModel() : "claude-sonnet-4-20250514";
	}

	private String formatPrompt(AgentTaskRequest request) {
		StringBuilder prompt = new StringBuilder();
		if (request.workingDirectory() != null) {
			prompt.append("You are working in directory: ")
				.append(request.workingDirectory().toString())
				.append("\n\n");
		}
		prompt.append("Task: ").append(request.goal()).append("\n\n");
		prompt.append("Instructions:\n");
		prompt.append("1. Analyze the files in the working directory\n");
		prompt.append("2. Complete the requested task by making necessary changes\n");
		prompt.append("3. Ensure the changes fix the problem\n\n");
		return prompt.toString();
	}

	private AgentResponse createErrorResponse(String errorMessage, Duration duration) {
		AgentGenerationMetadata generationMetadata = new AgentGenerationMetadata("ERROR", Map.of());
		List<AgentGeneration> generations = List.of(new AgentGeneration(errorMessage, generationMetadata));

		AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
			.model(getEffectiveModel())
			.duration(duration)
			.build();

		return new AgentResponse(generations, responseMetadata);
	}

	// ========== Builder ==========

	/**
	 * Builder for ClaudeAgentModel.
	 */
	public static class Builder {

		private Path workingDirectory;

		private Duration timeout = Duration.ofMinutes(10);

		private String claudePath;

		private HookRegistry hookRegistry;

		private ClaudeAgentOptions defaultOptions;

		private Executor asyncExecutor;

		private Builder() {
		}

		/**
		 * Sets the working directory for CLI execution.
		 * @param workingDirectory the working directory
		 * @return this builder
		 */
		public Builder workingDirectory(Path workingDirectory) {
			this.workingDirectory = workingDirectory;
			return this;
		}

		/**
		 * Sets the default timeout for operations.
		 * @param timeout the timeout duration
		 * @return this builder
		 */
		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * Sets the path to the Claude CLI executable.
		 * @param claudePath the path to Claude CLI
		 * @return this builder
		 */
		public Builder claudePath(String claudePath) {
			this.claudePath = claudePath;
			return this;
		}

		/**
		 * Sets a pre-configured hook registry.
		 * @param hookRegistry the hook registry
		 * @return this builder
		 */
		public Builder hookRegistry(HookRegistry hookRegistry) {
			this.hookRegistry = hookRegistry;
			return this;
		}

		/**
		 * Sets the default agent options.
		 * @param defaultOptions the default options
		 * @return this builder
		 */
		public Builder defaultOptions(ClaudeAgentOptions defaultOptions) {
			this.defaultOptions = defaultOptions;
			return this;
		}

		/**
		 * Sets the executor for async operations (streaming, iteration).
		 * <p>
		 * By default, uses a cached thread pool with daemon threads. Java 21 users can
		 * provide {@code Executors.newVirtualThreadPerTaskExecutor()} for virtual thread
		 * support.
		 * </p>
		 * <pre>{@code
		 * // Java 21 with virtual threads:
		 * ClaudeAgentModel.builder()
		 *     .asyncExecutor(Executors.newVirtualThreadPerTaskExecutor())
		 *     .build();
		 * }</pre>
		 * @param asyncExecutor the executor for async operations
		 * @return this builder
		 */
		public Builder asyncExecutor(Executor asyncExecutor) {
			this.asyncExecutor = asyncExecutor;
			return this;
		}

		/**
		 * Builds the ClaudeAgentModel.
		 * @return the configured model
		 */
		public ClaudeAgentModel build() {
			if (workingDirectory == null) {
				workingDirectory = Path.of(System.getProperty("user.dir"));
			}
			return new ClaudeAgentModel(this);
		}

	}

}
