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
import org.springaicommunity.agents.claude.sdk.parsing.RobustStreamParser;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.SystemMessage;
import org.springaicommunity.agents.claude.sdk.types.TextBlock;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for robust streaming implementation (Phase 4.8).
 *
 * <p>
 * These tests validate that our enhanced streaming components:
 * </p>
 * <ul>
 * <li>Parse stream-json correctly with character-based accumulation</li>
 * <li>Validate message flow matches expected protocol</li>
 * <li>Handle edge cases and error scenarios gracefully</li>
 * <li>Provide comprehensive diagnostics and monitoring</li>
 * </ul>
 */
class RobustStreamingIT extends ClaudeCliTestBase {

	private static final Logger logger = LoggerFactory.getLogger(RobustStreamingIT.class);

	@Test
	void testRobustStreamingBasicFlow() throws Exception {
		String prompt = "What is 2+2? Be concise.";
		List<Message> messages = new ArrayList<>();
		Instant startTime = Instant.now();

		logger.info("Testing robust streaming with prompt: {}", prompt);

		// Create robust streaming processor
		RobustStreamingProcessor processor = new RobustStreamingProcessor(messages::add, OutputFormat.STREAM_JSON);

		// Execute Claude CLI with stream-json format
		ProcessResult result = new ProcessExecutor()
			.command(getClaudeCliPath(), "--output-format", "stream-json", "--verbose", "--print", prompt)
			.redirectOutput(processor)
			.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java")
			.timeout(30, TimeUnit.SECONDS)
			.execute();

		Duration duration = Duration.between(startTime, Instant.now());
		processor.close();

		// Validate process success
		assertThat(result.getExitValue()).isEqualTo(0);

		// Validate message flow: SystemMessage -> AssistantMessage -> ResultMessage
		assertThat(messages).hasSizeGreaterThanOrEqualTo(3);
		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);

		// Find assistant and result messages
		List<AssistantMessage> assistantMessages = messages.stream()
			.filter(msg -> msg instanceof AssistantMessage)
			.map(msg -> (AssistantMessage) msg)
			.toList();

		List<ResultMessage> resultMessages = messages.stream()
			.filter(msg -> msg instanceof ResultMessage)
			.map(msg -> (ResultMessage) msg)
			.toList();

		assertThat(assistantMessages).isNotEmpty();
		assertThat(resultMessages).hasSize(1);

		// Validate system message
		SystemMessage systemMessage = (SystemMessage) messages.get(0);
		assertThat(systemMessage.subtype()).isEqualTo("init");
		assertThat(systemMessage.data()).containsKey("session_id");

		// Validate assistant message
		AssistantMessage assistantMessage = assistantMessages.get(0);
		assertThat(assistantMessage.content()).isNotEmpty();
		assertThat(assistantMessage.getTextContent()).isPresent();
		assertThat(assistantMessage.getTextContent().get()).contains("4");

		// Validate result message
		ResultMessage resultMessage = resultMessages.get(0);
		assertThat(resultMessage.subtype()).isEqualTo("success");
		assertThat(resultMessage.isError()).isFalse();
		assertThat(resultMessage.sessionId()).isNotNull();
		assertThat(resultMessage.result()).contains("4");

		// Get processor statistics
		RobustStreamingProcessor.StreamingStatistics stats = processor.getStatistics();
		assertThat(stats.messagesEmitted()).isEqualTo(messages.size());
		assertThat(stats.errors()).isEqualTo(0);

		logger.info("Robust streaming test completed successfully:");
		logger.info("  Duration: {} ms", duration.toMillis());
		logger.info("  Messages: {} (expected 3)", messages.size());
		logger.info("  Flow: {} -> {} -> {}", messages.get(0).getClass().getSimpleName(),
				assistantMessages.get(0).getClass().getSimpleName(), resultMessages.get(0).getClass().getSimpleName());
		logger.info("  Processor stats: {} lines, {} messages, {} errors", stats.linesProcessed(),
				stats.messagesEmitted(), stats.errors());
	}

	@Test
	void testRobustStreamingMultiplePrompts() throws Exception {
		String[] prompts = { "What is 3+3?", "List 2 colors.", "Say hello." };

		for (int i = 0; i < prompts.length; i++) {
			String prompt = prompts[i];
			logger.info("Testing prompt {}/{}: {}", i + 1, prompts.length, prompt);

			List<Message> messages = new ArrayList<>();
			RobustStreamingProcessor processor = new RobustStreamingProcessor(messages::add, OutputFormat.STREAM_JSON);

			ProcessResult result = new ProcessExecutor()
				.command(getClaudeCliPath(), "--output-format", "stream-json", "--verbose", "--print", prompt)
				.redirectOutput(processor)
				.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java")
				.timeout(30, TimeUnit.SECONDS)
				.execute();

			processor.close();

			// Validate each test
			assertThat(result.getExitValue()).isEqualTo(0);
			assertThat(messages).hasSizeGreaterThanOrEqualTo(3);

			// Validate message types
			assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
			assertThat(messages.stream().anyMatch(msg -> msg instanceof AssistantMessage)).isTrue();
			assertThat(messages.stream().anyMatch(msg -> msg instanceof ResultMessage)).isTrue();

			logger.info("  Prompt {} completed: {} messages", i + 1, messages.size());
		}
	}

	@Test
	void testStreamingErrorRecovery() throws Exception {
		// Test that streaming processor can handle malformed input gracefully
		List<Message> messages = new ArrayList<>();
		RobustStreamingProcessor processor = new RobustStreamingProcessor(messages::add, OutputFormat.STREAM_JSON);

		// Simulate individual error scenarios that should be handled gracefully
		processor.processLine("invalid line that should be ignored"); // Non-JSON line -
																		// should be
																		// ignored
		processor.close(); // Close to flush any incomplete buffer

		// Create new processor for the valid message test
		messages.clear();
		RobustStreamingProcessor processor2 = new RobustStreamingProcessor(messages::add, OutputFormat.STREAM_JSON);

		processor2.processLine("{\"type\":\"system\",\"subtype\":\"init\",\"session_id\":\"test123\"}"); // Valid
																											// message
		processor2.close();

		RobustStreamingProcessor.StreamingStatistics stats = processor2.getStatistics();

		// Should have processed the valid message line
		assertThat(stats.linesProcessed()).isEqualTo(1);
		// Should have parsed the valid system message
		assertThat(stats.messagesEmitted()).isGreaterThanOrEqualTo(1);

		// Verify message was actually added to the list
		assertThat(messages).isNotEmpty();

		logger.info("Error recovery test completed:");
		logger.info("  Lines processed: {}", stats.linesProcessed());
		logger.info("  Messages emitted: {}", stats.messagesEmitted());
		logger.info("  Errors handled: {}", stats.errors());
	}

	@Test
	void testParserBufferManagement() throws Exception {
		// Test that the robust parser properly manages large inputs
		RobustStreamParser parser = new RobustStreamParser();

		// Test normal accumulation with valid Claude message structure
		assertThat(parser.accumulateAndParse("{\"type\":\"system\",\"subtype\":")).isEmpty();
		assertThat(parser.accumulateAndParse("\"init\",\"session_id\":\"test123\"}")).isPresent();

		// Test buffer limits (should handle gracefully)
		StringBuilder largeJson = new StringBuilder(
				"{\"type\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"");
		for (int i = 0; i < 100000; i++) {
			largeJson.append("x");
		}
		largeJson.append("\"}]}");

		// This should either parse successfully or fail gracefully due to buffer limits
		try {
			parser.accumulateAndParse(largeJson.toString());
		}
		catch (Exception e) {
			// Buffer limit handling is expected for very large inputs
			logger.info("Large input handled with: {}", e.getClass().getSimpleName());
		}

		RobustStreamParser.ParsingStats stats = parser.getStats();
		assertThat(stats.parseAttempts()).isGreaterThan(0);

		logger.info("Buffer management test completed:");
		logger.info("  Parse attempts: {}", stats.parseAttempts());
		logger.info("  Successful parses: {}", stats.successfulParses());
		logger.info("  Success rate: {:.2f}%", stats.getSuccessRate() * 100);
	}

	@Test
	void testStreamingStateMachine() throws Exception {
		// Test that state machine properly validates message flow
		StreamingStateMachine stateMachine = new StreamingStateMachine();

		// Create mock messages in proper order
		SystemMessage systemMsg = SystemMessage.of("init", java.util.Map.of("session_id", "test123"));

		AssistantMessage assistantMsg = AssistantMessage.of(List.of(new TextBlock("Test response")));

		ResultMessage resultMsg = ResultMessage.builder()
			.subtype("success")
			.sessionId("test123")
			.isError(false)
			.numTurns(1)
			.durationMs(1000)
			.durationApiMs(800)
			.result("Test response")
			.build();

		// Process messages in correct order
		stateMachine.processMessage(systemMsg);
		assertThat(stateMachine.getCurrentState()).isEqualTo(StreamingStateMachine.State.AWAITING_CONTENT);

		stateMachine.processMessage(assistantMsg);
		assertThat(stateMachine.getCurrentState()).isEqualTo(StreamingStateMachine.State.AWAITING_CONTENT);

		stateMachine.processMessage(resultMsg);
		assertThat(stateMachine.getCurrentState()).isEqualTo(StreamingStateMachine.State.COMPLETED);
		assertThat(stateMachine.isComplete()).isTrue();

		// Validate completion
		StreamingStateMachine.StreamCompletionSummary summary = stateMachine.validateCompletion();
		assertThat(summary.totalMessages()).isEqualTo(3);
		assertThat(summary.sessionId()).isEqualTo("test123");
		assertThat(summary.hasAssistantResponse()).isTrue();

		logger.info("State machine test completed:");
		logger.info("  Final state: {}", stateMachine.getCurrentState());
		logger.info("  Total messages: {}", summary.totalMessages());
		logger.info("  Session ID: {}", summary.sessionId());
	}

}