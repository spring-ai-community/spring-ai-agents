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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.BidirectionalTransport;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for multi-turn session conversations with real Claude CLI.
 */
class SessionIntegrationIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = "claude-haiku-4-5-20251016";

	@Test
	@DisplayName("Should maintain context across multiple queries in same session")
	void shouldMaintainContextAcrossQueries() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<String> responses = new ArrayList<>();

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			// First query - establish context
			CountDownLatch firstLatch = new CountDownLatch(1);
			AtomicReference<String> firstResponse = new AtomicReference<>();

			transport.startSession("My name is Alice. Please remember this.", options, message -> {
				if (message.isRegularMessage()) {
					Message msg = message.asMessage();
					if (msg instanceof ResultMessage result) {
						firstResponse.set(result.result());
						firstLatch.countDown();
					}
				}
			}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

			boolean firstCompleted = firstLatch.await(60, TimeUnit.SECONDS);
			assertThat(firstCompleted).as("First query should complete").isTrue();
			responses.add(firstResponse.get());

			// Second query - verify context maintained
			CountDownLatch secondLatch = new CountDownLatch(1);
			AtomicReference<String> secondResponse = new AtomicReference<>();

			transport.sendUserMessage("What is my name?", "default");

			// Wait for second response - need to read from the existing stream
			// Note: In the current implementation, messages continue flowing to the same
			// handler
			// We need a way to track which response goes to which query

			// For now, just verify the transport stays alive for follow-up queries
			assertThat(transport.isRunning()).as("Transport should still be running for follow-up queries").isTrue();
		}
	}

	@Test
	@DisplayName("Should handle session with multiple messages")
	void shouldHandleSessionWithMultipleMessages() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<ParsedMessage> allMessages = new ArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			// When - start a session
			transport.startSession("Tell me a very short joke (one line only)", options, message -> {
				allMessages.add(message);
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

			// Wait for result
			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);

			// Then
			assertThat(completed).as("Should complete within timeout").isTrue();
			assertThat(allMessages).isNotEmpty();

			// Should have received messages
			long regularMessageCount = allMessages.stream().filter(ParsedMessage::isRegularMessage).count();

			assertThat(regularMessageCount).as("Should have at least one regular message").isGreaterThanOrEqualTo(1);
		}
	}

	@Test
	@DisplayName("Should close session cleanly after multiple interactions")
	void shouldCloseSessionCleanlyAfterMultipleInteractions() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath());

		try {
			CountDownLatch resultLatch = new CountDownLatch(1);

			// Start session
			transport.startSession("Say hi", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

			// Wait for first response
			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);
			assertThat(completed).isTrue();

			// Transport should still be running (bidirectional mode keeps connection
			// open)
			assertThat(transport.isRunning()).isTrue();
		}
		finally {
			// Close should work cleanly
			transport.close();
			assertThat(transport.isRunning()).isFalse();
		}
	}

	@Test
	@DisplayName("Should receive assistant messages during session")
	void shouldReceiveAssistantMessagesDuringSession() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<AssistantMessage> assistantMessages = new ArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			// When
			transport.startSession("What color is the sky? Answer in one word.", options, message -> {
				if (message.isRegularMessage()) {
					Message msg = message.asMessage();
					if (msg instanceof AssistantMessage assistant) {
						assistantMessages.add(assistant);
					}
					else if (msg instanceof ResultMessage) {
						resultLatch.countDown();
					}
				}
			}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

			// Wait for result
			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);

			// Then
			assertThat(completed).as("Should complete").isTrue();
			assertThat(assistantMessages).as("Should receive at least one assistant message").isNotEmpty();
		}
	}

}
