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

package org.springaicommunity.agents.claude.sdk.transport;

import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.exceptions.TransportException;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.resilience.ResilienceManager;
import org.springaicommunity.agents.claude.sdk.resilience.RetryConfiguration;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive wrapper providing true non-blocking streaming for one-shot queries.
 *
 * <p>
 * This transport uses {@link BidirectionalTransport} internally for robust message
 * handling, while exposing a simple reactive API for single queries. For multi-turn
 * conversations, use
 * {@link org.springaicommunity.agents.claude.sdk.session.ClaudeSession} directly.
 * </p>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>True non-blocking with backpressure support</li>
 * <li>Resilience via retry and circuit breaker patterns</li>
 * <li>Automatic session lifecycle management</li>
 * <li>Uses robust BidirectionalTransport internally</li>
 * </ul>
 */
public class ReactiveTransport implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ReactiveTransport.class);

	private final Path workingDirectory;

	private final Duration defaultTimeout;

	private final String claudePath;

	private final ResilienceManager resilienceManager;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	public ReactiveTransport(Path workingDirectory) {
		this(workingDirectory, Duration.ofMinutes(2));
	}

	public ReactiveTransport(Path workingDirectory, Duration defaultTimeout) {
		this(workingDirectory, defaultTimeout, null);
	}

	public ReactiveTransport(Path workingDirectory, Duration defaultTimeout, String claudePath) {
		this.workingDirectory = workingDirectory;
		this.defaultTimeout = defaultTimeout;
		this.claudePath = claudePath;
		this.resilienceManager = new ResilienceManager(RetryConfiguration.defaultNetwork(), defaultTimeout);
	}

	/**
	 * Executes a reactive query returning a Flux of messages as they arrive. True
	 * non-blocking implementation with backpressure support and resilience.
	 *
	 * <p>
	 * This method creates an ephemeral session using BidirectionalTransport, streams
	 * messages until completion, and cleans up automatically.
	 * </p>
	 * @param prompt the prompt to send
	 * @param options CLI options for the query
	 * @return Flux of messages that completes when the query finishes
	 */
	public Flux<Message> executeReactiveQuery(String prompt, CLIOptions options) {
		return resilienceManager.executeResilientFlux("reactive-query",
				() -> executeReactiveQueryInternal(prompt, options));
	}

	/**
	 * Internal implementation using BidirectionalTransport for robust streaming.
	 */
	private Flux<Message> executeReactiveQueryInternal(String prompt, CLIOptions options) {
		if (closed.get()) {
			return Flux.error(new ClaudeSDKException("Transport is closed"));
		}

		return Flux.defer(() -> {
			logger.info("Starting reactive query with BidirectionalTransport");

			// Create ephemeral transport for this query
			BidirectionalTransport transport = new BidirectionalTransport(workingDirectory,
					options.getTimeout() != null ? options.getTimeout() : defaultTimeout, claudePath, null // no
																											// sandbox
																											// for
																											// reactive
																											// queries
			);

			// Start session with null handlers - we just consume messages
			try {
				transport.startSession(prompt, options, msg -> {
				}, // messages go to inbound sink
						request -> {
							// For reactive queries, auto-allow all tool usage
							return org.springaicommunity.agents.claude.sdk.types.control.ControlResponse
								.success(request.requestId(), java.util.Map.of("behavior", "allow"));
						});
			}
			catch (Exception e) {
				transport.close();
				return Flux.error(new TransportException("Failed to start reactive session", e));
			}

			// Get the inbound flux and transform ParsedMessage -> Message
			return transport.getInboundFlux()
				.filter(ParsedMessage::isRegularMessage)
				.map(ParsedMessage::asMessage)
				.takeUntil(msg -> msg instanceof ResultMessage)
				.doOnSubscribe(sub -> logger.debug("Subscribed to reactive query"))
				.doOnComplete(() -> logger.debug("Reactive query completed"))
				.doOnError(err -> logger.error("Reactive query failed", err))
				.doFinally(signal -> {
					logger.debug("Closing ephemeral transport (signal: {})", signal);
					transport.close();
				});
		}).subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * Executes a reactive query returning a Mono with the complete result. Collects all
	 * messages and returns them as a list with resilience.
	 * @param prompt the prompt to send
	 * @param options CLI options for the query
	 * @return Mono containing list of all messages
	 */
	public Mono<List<Message>> executeReactiveQueryComplete(String prompt, CLIOptions options) {
		return resilienceManager.executeResilientMono("complete-query",
				() -> executeReactiveQuery(prompt, options).collectList()
					.timeout(options.getTimeout() != null ? options.getTimeout() : defaultTimeout)
					.doOnSuccess(messages -> logger.info("Collected {} messages", messages.size())));
	}

	/**
	 * Tests if Claude CLI is available reactively with resilience.
	 * @return Mono that emits true if CLI is available, false otherwise
	 */
	public Mono<Boolean> isAvailableAsync() {
		return resilienceManager.executeResilientMono("availability-check", () -> Mono.fromCallable(() -> {
			try {
				BidirectionalTransport transport = new BidirectionalTransport(workingDirectory, Duration.ofSeconds(10),
						claudePath, null);
				transport.close();
				return true;
			}
			catch (Exception e) {
				return false;
			}
		}).subscribeOn(Schedulers.boundedElastic())).onErrorReturn(false);
	}

	/**
	 * Gets the Claude CLI version reactively with resilience.
	 * @return Mono containing the CLI version string
	 */
	public Mono<String> getVersionAsync() {
		return resilienceManager.executeResilientMono("version-check", () -> Mono.fromCallable(() -> {
			// Use simple process execution for version check
			ProcessBuilder pb = new ProcessBuilder(claudePath != null ? claudePath : "claude", "--version");
			pb.directory(workingDirectory.toFile());
			Process process = pb.start();
			String version = new String(process.getInputStream().readAllBytes()).trim();
			process.waitFor();
			return version;
		}).subscribeOn(Schedulers.boundedElastic()));
	}

	/**
	 * Gets resilience manager metrics for monitoring.
	 * @return the resilience manager
	 */
	public ResilienceManager getResilienceManager() {
		return resilienceManager;
	}

	@Override
	public void close() {
		if (!closed.getAndSet(true)) {
			logger.debug("ReactiveTransport closed");
		}
	}

}
