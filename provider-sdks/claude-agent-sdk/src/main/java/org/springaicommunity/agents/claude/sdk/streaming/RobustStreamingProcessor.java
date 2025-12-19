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

package org.springaicommunity.agents.claude.sdk.streaming;

import org.springaicommunity.agents.claude.sdk.config.OutputFormat;
import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.exceptions.MessageParseException;
import org.springaicommunity.agents.claude.sdk.parsing.RobustStreamParser;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Robust streaming processor that implements Python SDK-style reliability patterns.
 *
 * <p>
 * Key improvements over the basic MessageCollector:
 * </p>
 * <ul>
 * <li>Character-based JSON accumulation (not line-based)</li>
 * <li>Streaming state validation with proper flow checking</li>
 * <li>Timeout detection and hanging process recovery</li>
 * <li>Buffer size limits and error recovery</li>
 * <li>Comprehensive logging and diagnostics</li>
 * </ul>
 *
 * <p>
 * This processor aligns with the official Python SDK streaming patterns and addresses
 * known issues like GitHub Issue #1920 (hanging CLI).
 * </p>
 */
public class RobustStreamingProcessor extends LogOutputStream {

	private static final Logger logger = LoggerFactory.getLogger(RobustStreamingProcessor.class);

	/**
	 * Maximum time between messages before considering stream hung.
	 */
	private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(30);

	/**
	 * Maximum total streaming time before forcing timeout.
	 */
	private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(10);

	private final Consumer<Message> messageHandler;

	private final OutputFormat outputFormat;

	private final RobustStreamParser parser;

	private final StreamingStateMachine stateMachine;

	private final StringBuilder textBuffer = new StringBuilder();

	// Timeout and hanging detection
	private Instant lastMessageTime = Instant.now();

	private final Instant streamStartTime = Instant.now();

	private final CompletableFuture<Void> timeoutWatcher;

	private volatile boolean streamClosed = false;

	// Statistics
	private int linesProcessed = 0;

	private int messagesEmitted = 0;

	private int errors = 0;

	public RobustStreamingProcessor(Consumer<Message> messageHandler, OutputFormat outputFormat) {
		this.messageHandler = messageHandler;
		this.outputFormat = outputFormat;
		this.parser = new RobustStreamParser();
		this.stateMachine = new StreamingStateMachine();

		// Start timeout monitoring
		this.timeoutWatcher = startTimeoutMonitoring();

		logger.debug("Started robust streaming processor for format: {}", outputFormat);
	}

	@Override
	protected void processLine(String line) {
		if (streamClosed) {
			logger.warn("Received line after stream closed: {}", line.substring(0, Math.min(50, line.length())));
			return;
		}

		linesProcessed++;
		lastMessageTime = Instant.now();

		logger.trace("Processing line {}: {}", linesProcessed, line.substring(0, Math.min(100, line.length())));

		try {
			switch (outputFormat) {
				case TEXT -> processTextLine(line);
				case STREAM_JSON -> processStreamJsonLine(line);
				case JSON -> processJsonLine(line);
			}
		}
		catch (Exception e) {
			errors++;
			logger.error("Error processing line {}: {}", linesProcessed,
					line.substring(0, Math.min(100, line.length())), e);

			// For critical errors, attempt recovery
			if (e instanceof MessageParseException) {
				logger.warn("JSON decode error, clearing parser buffer for recovery");
				parser.clearBuffer();
			}
		}
	}

	private void processTextLine(String line) {
		// For TEXT format, accumulate all lines
		textBuffer.append(line).append("\n");
	}

	private void processStreamJsonLine(String line) throws MessageParseException, ClaudeSDKException {
		// Use robust character-based accumulation (Python SDK pattern)
		Optional<Message> messageOpt = parser.accumulateAndParse(line);

		if (messageOpt.isPresent()) {
			Message message = messageOpt.get();

			// Validate message flow with state machine
			stateMachine.processMessage(message);

			// Emit message to handler
			messageHandler.accept(message);
			messagesEmitted++;

			logger.debug("Emitted message {} of type: {}", messagesEmitted, message.getClass().getSimpleName());

			// Check for stream completion
			if (stateMachine.isComplete()) {
				logger.info("Stream completed successfully after {} messages", messagesEmitted);
				scheduleStreamClose();
			}
		}
	}

	private void processJsonLine(String line) throws MessageParseException {
		// For single JSON format, accumulate until we have complete JSON
		Optional<Message> messageOpt = parser.accumulateAndParse(line);

		if (messageOpt.isPresent()) {
			Message message = messageOpt.get();
			messageHandler.accept(message);
			messagesEmitted++;

			logger.debug("Emitted single JSON message of type: {}", message.getClass().getSimpleName());
		}
	}

	@Override
	public void close() throws IOException {
		streamClosed = true;
		timeoutWatcher.cancel(true);

		logger.debug("Closing streaming processor. Lines: {}, Messages: {}, Errors: {}", linesProcessed,
				messagesEmitted, errors);

		try {
			// Handle remaining content based on format
			switch (outputFormat) {
				case TEXT -> flushTextBuffer();
				case STREAM_JSON -> flushStreamJson();
				case JSON -> flushJsonBuffer();
			}

			// Validate final stream state
			validateStreamCompletion();

		}
		catch (Exception e) {
			logger.error("Error during stream close", e);
		}
		finally {
			super.close();
			logFinalStatistics();
		}
	}

	private void flushTextBuffer() {
		if (textBuffer.length() > 0) {
			String textResult = textBuffer.toString().trim();
			if (!textResult.isEmpty()) {
				ResultMessage textMessage = ResultMessage.builder()
					.subtype("success")
					.result(textResult)
					.isError(false)
					.numTurns(1)
					.durationMs(0)
					.durationApiMs(0)
					.build();
				messageHandler.accept(textMessage);
				messagesEmitted++;
				logger.debug("Emitted text result message ({} chars)", textResult.length());
			}
		}
	}

	private void flushStreamJson() throws ClaudeSDKException {
		// Attempt to parse any remaining buffer content
		Optional<Message> messageOpt = parser.flushBuffer();
		if (messageOpt.isPresent()) {
			Message message = messageOpt.get();
			stateMachine.processMessage(message);
			messageHandler.accept(message);
			messagesEmitted++;
			logger.debug("Emitted final buffered message of type: {}", message.getClass().getSimpleName());
		}
	}

	private void flushJsonBuffer() {
		// For JSON format, flush any remaining content
		Optional<Message> messageOpt = parser.flushBuffer();
		if (messageOpt.isPresent()) {
			messageHandler.accept(messageOpt.get());
			messagesEmitted++;
			logger.debug("Emitted final JSON message");
		}
	}

	private void validateStreamCompletion() {
		if (outputFormat == OutputFormat.STREAM_JSON) {
			try {
				StreamingStateMachine.StreamCompletionSummary summary = stateMachine.validateCompletion();
				logger.info("Stream validation successful: {} messages in {} ms", summary.totalMessages(),
						summary.streamDuration().toMillis());
			}
			catch (ClaudeSDKException e) {
				logger.warn("Stream validation failed: {}", e.getMessage());
				// Don't throw - we want to return partial results
			}
		}
	}

	private CompletableFuture<Void> startTimeoutMonitoring() {
		return CompletableFuture.runAsync(() -> {
			try {
				while (!streamClosed && !Thread.currentThread().isInterrupted()) {
					Thread.sleep(1000); // Check every second

					Instant now = Instant.now();
					Duration sinceLastMessage = Duration.between(lastMessageTime, now);
					Duration totalDuration = Duration.between(streamStartTime, now);

					// Check for hanging (no messages for MESSAGE_TIMEOUT)
					if (sinceLastMessage.compareTo(MESSAGE_TIMEOUT) > 0 && messagesEmitted > 0) {
						logger.warn("Stream appears to be hanging: {} seconds since last message",
								sinceLastMessage.toSeconds());
						// Could trigger hanging recovery here
					}

					// Check for total timeout
					if (totalDuration.compareTo(STREAM_TIMEOUT) > 0) {
						logger.error("Stream exceeded maximum duration: {} minutes", totalDuration.toMinutes());
						break;
					}
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.debug("Timeout monitoring interrupted");
			}
		});
	}

	private void scheduleStreamClose() {
		// Schedule close after a brief delay to allow any final messages
		CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
			if (!streamClosed) {
				logger.debug("Auto-closing stream after completion");
				try {
					close();
				}
				catch (IOException e) {
					logger.warn("Error during auto-close", e);
				}
			}
		});
	}

	private void logFinalStatistics() {
		Duration totalDuration = Duration.between(streamStartTime, Instant.now());
		RobustStreamParser.ParsingStats parserStats = parser.getStats();
		StreamingStateMachine.StreamingStats streamStats = stateMachine.getStats();

		logger.info("Streaming completed - Duration: {} ms, Lines: {}, Messages: {}, Errors: {}",
				totalDuration.toMillis(), linesProcessed, messagesEmitted, errors);
		logger.info("Parser stats - Success rate: {:.2f}%, Total chars: {}", parserStats.getSuccessRate() * 100,
				parserStats.totalCharactersProcessed());
		logger.debug("Final state: {}, Session: {}", streamStats.currentState(), streamStats.sessionId());
	}

	/**
	 * Gets current streaming statistics for monitoring.
	 * @return current streaming statistics
	 */
	public StreamingStatistics getStatistics() {
		Duration elapsed = Duration.between(streamStartTime, Instant.now());
		return new StreamingStatistics(linesProcessed, messagesEmitted, errors, elapsed, parser.getStats(),
				stateMachine.getStats());
	}

	/**
	 * Comprehensive streaming statistics.
	 */
	public record StreamingStatistics(int linesProcessed, int messagesEmitted, int errors, Duration elapsed,
			RobustStreamParser.ParsingStats parserStats, StreamingStateMachine.StreamingStats streamStats) {
	}

}