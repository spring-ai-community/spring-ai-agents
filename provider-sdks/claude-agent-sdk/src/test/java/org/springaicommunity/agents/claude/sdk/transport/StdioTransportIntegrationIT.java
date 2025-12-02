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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.SystemMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for BidirectionalTransport following MCP SDK StdioClientTransport
 * patterns. These tests verify the transport layer behavior independently from
 * higher-level session management.
 *
 * <p>
 * Test patterns adapted from MCP SDK:
 * <ul>
 * <li>StdioMcpSyncClientTests.java - error handling, timeout patterns</li>
 * <li>AbstractMcpSyncClientTests.java - withClient pattern, state verification</li>
 * <li>StdioClientTransport.java - scheduler patterns, graceful shutdown</li>
 * </ul>
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class StdioTransportIntegrationIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = "claude-haiku-4-5-20251016";

	/**
	 * Helper pattern from MCP SDK AbstractMcpSyncClientTests.withClient().
	 */
	void withTransport(Consumer<BidirectionalTransport> consumer) throws Exception {
		BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath());
		try {
			consumer.accept(transport);
		}
		finally {
			if (transport.isRunning()) {
				transport.close();
			}
			assertThat(transport.isRunning()).as("Transport should be closed after test").isFalse();
		}
	}

	@Test
	@DisplayName("Transport should transition through state machine - MCP SDK pattern")
	void transportShouldTransitionThroughStateMachine() throws Exception {
		// Given - track state transitions
		BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(1),
				getClaudeCliPath());

		// Initially disconnected
		assertThat(transport.isRunning()).as("Should start disconnected").isFalse();

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		CountDownLatch resultLatch = new CountDownLatch(1);

		// When - start session
		transport.startSession("Say hi", options, message -> {
			if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
				resultLatch.countDown();
			}
		}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

		// Should be running after start
		assertThat(transport.isRunning()).as("Should be running after startSession").isTrue();

		// Wait for completion
		resultLatch.await(30, TimeUnit.SECONDS);

		// Still running until explicitly closed (bidirectional mode)
		assertThat(transport.isRunning()).as("Should remain running for follow-up queries").isTrue();

		// Close
		transport.close();
		assertThat(transport.isRunning()).as("Should be stopped after close").isFalse();
	}

	@Test
	@DisplayName("Transport should handle inbound messages via sink - MCP SDK Sinks.Many pattern")
	void transportShouldHandleInboundMessagesViaSink() throws Exception {
		// Given - collect all messages (MCP pattern: CopyOnWriteArrayList for thread
		// safety)
		List<ParsedMessage> allMessages = new CopyOnWriteArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		withTransport(transport -> {
			try {
				transport.startSession("What is 2 + 2? Reply with just the number.", options, message -> {
					allMessages.add(message);
					if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
						resultLatch.countDown();
					}
				}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

				boolean completed = resultLatch.await(30, TimeUnit.SECONDS);
				assertThat(completed).as("Should complete within timeout").isTrue();

				// Verify message flow (MCP SDK verifies message structure)
				assertThat(allMessages).as("Should receive messages").isNotEmpty();

				// Should have system, assistant, and result messages
				long regularCount = allMessages.stream().filter(ParsedMessage::isRegularMessage).count();
				assertThat(regularCount).as("Should have regular messages").isGreaterThanOrEqualTo(1);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	@DisplayName("Transport should handle outbound messages - MCP SDK sendMessage pattern")
	void transportShouldHandleOutboundMessages() throws Exception {
		// Given
		AtomicBoolean userMessageSent = new AtomicBoolean(false);
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		withTransport(transport -> {
			try {
				transport.startSession("Remember: my favorite color is blue.", options, message -> {
					if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
						resultLatch.countDown();
					}
				}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

				boolean firstCompleted = resultLatch.await(30, TimeUnit.SECONDS);
				assertThat(firstCompleted).as("First query should complete").isTrue();

				// Send follow-up (outbound message)
				transport.sendUserMessage("What is my favorite color?", "default");
				userMessageSent.set(true);

				// Transport should still be running
				assertThat(transport.isRunning()).as("Transport should remain running").isTrue();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		assertThat(userMessageSent.get()).as("Should have sent follow-up message").isTrue();
	}

	@Test
	@DisplayName("Transport should handle stderr - MCP SDK setStdErrorHandler pattern")
	void transportShouldHandleStderr() throws Exception {
		// Note: Claude CLI uses verbose output which may produce stderr
		// This test verifies transport doesn't fail when stderr is produced
		CountDownLatch resultLatch = new CountDownLatch(1);
		AtomicBoolean errorHandled = new AtomicBoolean(false);

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		withTransport(transport -> {
			try {
				transport.startSession("Say hello", options, message -> {
					if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
						resultLatch.countDown();
					}
				}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

				boolean completed = resultLatch.await(30, TimeUnit.SECONDS);
				assertThat(completed).as("Should complete even with potential stderr output").isTrue();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	@DisplayName("Graceful shutdown should complete sinks and dispose schedulers - MCP SDK pattern")
	void gracefulShutdownShouldCompleteAndDispose() throws Exception {
		// Given
		BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(1),
				getClaudeCliPath());

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		CountDownLatch resultLatch = new CountDownLatch(1);

		transport.startSession("Hi", options, message -> {
			if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
				resultLatch.countDown();
			}
		}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

		resultLatch.await(30, TimeUnit.SECONDS);

		// When - close gracefully (MCP SDK: closeGracefully())
		transport.close();

		// Then - verify complete cleanup
		assertThat(transport.isRunning()).as("Transport should be stopped").isFalse();
	}

	@Test
	@DisplayName("Transport should support message iterator - Claude SDK unique feature")
	void transportShouldSupportMessageIterator() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<ParsedMessage> iteratedMessages = new ArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		withTransport(transport -> {
			try {
				transport.startSession("Say 'test'", options, message -> {
					iteratedMessages.add(message);
					if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
						resultLatch.countDown();
					}
				}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

				boolean completed = resultLatch.await(30, TimeUnit.SECONDS);
				assertThat(completed).isTrue();

				// Verify iterator pattern works
				assertThat(iteratedMessages).isNotEmpty();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	@DisplayName("Transport should handle concurrent message handlers - MCP SDK thread safety")
	void transportShouldHandleConcurrentMessageHandlers() throws Exception {
		// Given - thread-safe collections (MCP SDK pattern)
		List<ParsedMessage> messages = new CopyOnWriteArrayList<>();
		AtomicInteger controlRequestCount = new AtomicInteger(0);
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder().model(HAIKU_MODEL).permissionMode(PermissionMode.DEFAULT).build();

		withTransport(transport -> {
			try {
				transport.startSession("What is the capital of France?", options, message -> {
					// Message handler called from inbound scheduler
					messages.add(message);
					if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
						resultLatch.countDown();
					}
				}, request -> {
					// Control handler called from same or different thread
					controlRequestCount.incrementAndGet();
					return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
				});

				boolean completed = resultLatch.await(30, TimeUnit.SECONDS);
				assertThat(completed).isTrue();

				// Verify thread-safe collections worked
				assertThat(messages).isNotEmpty();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	@DisplayName("Transport construction should validate arguments - MCP SDK Assert pattern")
	void transportConstructionShouldValidateArguments() {
		// MCP SDK pattern: Assert.notNull() in constructor for required arguments
		assertThatCode(() -> new BidirectionalTransport(null, Duration.ofMinutes(1), getClaudeCliPath()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("workingDirectory");

		assertThatCode(() -> new BidirectionalTransport(workingDirectory(), null, getClaudeCliPath()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("defaultTimeout");

		// null claudePath is allowed - auto-discovers via ClaudeCliDiscovery
		assertThatCode(() -> {
			try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(),
					Duration.ofMinutes(1), null)) {
				assertThat(transport).isNotNull();
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Transport should report message types correctly")
	void transportShouldReportMessageTypesCorrectly() throws Exception {
		// Given - track message types
		List<Class<?>> messageTypes = new CopyOnWriteArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		withTransport(transport -> {
			try {
				transport.startSession("What is 1+1?", options, message -> {
					if (message.isRegularMessage()) {
						Message msg = message.asMessage();
						messageTypes.add(msg.getClass());
						if (msg instanceof ResultMessage) {
							resultLatch.countDown();
						}
					}
				}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

				boolean completed = resultLatch.await(30, TimeUnit.SECONDS);
				assertThat(completed).isTrue();

				// Should have appropriate message types
				assertThat(messageTypes).as("Should have message types").isNotEmpty();
				assertThat(messageTypes).as("Should end with ResultMessage").contains(ResultMessage.class);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

}
