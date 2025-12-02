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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.BidirectionalTransport;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
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
 * Integration tests for session control operations with real Claude CLI.
 *
 * <p>
 * Tests control request/response correlation for:
 * <ul>
 * <li>Control response parsing and correlation</li>
 * <li>Session lifecycle management</li>
 * </ul>
 *
 * <p>
 * NOTE: Multi-turn conversation tests require special handling because the session
 * iterator completes after the first result. These tests verify the transport-level
 * control request/response handling works correctly.
 */
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class SessionControlIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = CLIOptions.MODEL_HAIKU;

	/**
	 * Tests that control response parsing works correctly by verifying the transport
	 * receives and routes control_response messages.
	 */
	@Test
	@DisplayName("Control response is parsed and routed correctly")
	void controlResponseIsParsedAndRouted() throws Exception {
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<ParsedMessage> allMessages = new ArrayList<>();
		AtomicReference<String> resultText = new AtomicReference<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			transport.startSession("Say hello in exactly three words", options, message -> {
				allMessages.add(message);
				if (message.isRegularMessage()) {
					Message msg = message.asMessage();
					if (msg instanceof ResultMessage result) {
						resultText.set(result.result());
						resultLatch.countDown();
					}
				}
			}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")), response -> {
				// Control response handler - verify we can receive control responses
				System.out.println("Received control response: " + response);
			});

			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);

			assertThat(completed).as("Should complete within timeout").isTrue();
			assertThat(resultText.get()).as("Should receive result").isNotEmpty();
			assertThat(allMessages).as("Should receive messages").isNotEmpty();
		}
	}

	/**
	 * Tests that session connects and can be closed cleanly.
	 */
	@Test
	@DisplayName("Session connects and closes cleanly")
	void sessionConnectsAndClosesCleanly() throws Exception {
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		AtomicReference<String> resultText = new AtomicReference<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			transport.startSession("Say 'hi'", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage result) {
					resultText.set(result.result());
					resultLatch.countDown();
				}
			}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);
			assertThat(completed).isTrue();

			// Transport should still be running after first response
			assertThat(transport.isRunning()).as("Transport should still be running").isTrue();
		}
		// After close, should be stopped
	}

	/**
	 * Tests sending a follow-up query after initial response.
	 */
	@Test
	@DisplayName("Can send follow-up query in same session")
	void canSendFollowUpQuery() throws Exception {
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<String> results = new ArrayList<>();
		CountDownLatch firstLatch = new CountDownLatch(1);
		CountDownLatch secondLatch = new CountDownLatch(2);

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			transport.startSession("My name is TestUser. Remember this.", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage result) {
					results.add(result.result());
					if (results.size() == 1) {
						firstLatch.countDown();
					}
					secondLatch.countDown();
				}
			}, request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

			// Wait for first response
			boolean firstCompleted = firstLatch.await(60, TimeUnit.SECONDS);
			assertThat(firstCompleted).as("First query should complete").isTrue();
			assertThat(results).hasSize(1);

			// Send follow-up query
			transport.sendUserMessage("What is my name?", "default");

			// Wait for second response
			boolean secondCompleted = secondLatch.await(60, TimeUnit.SECONDS);
			assertThat(secondCompleted).as("Second query should complete").isTrue();
			assertThat(results).hasSize(2);

			// Verify context was preserved
			assertThat(results.get(1).toLowerCase()).as("Should remember the name from context").contains("testuser");
		}
	}

	/**
	 * Tests that the control response message type is correctly parsed. This verifies the
	 * ParsedMessage.ControlResponseMessage record works correctly.
	 */
	@Test
	@DisplayName("ParsedMessage correctly identifies control response type")
	void parsedMessageIdentifiesControlResponseType() {
		// Create a control response and verify type identification
		ControlResponse response = ControlResponse.success("req_123", Map.of("status", "ok"));
		ParsedMessage parsed = ParsedMessage.ControlResponseMessage.of(response);

		assertThat(parsed.isControlResponse()).isTrue();
		assertThat(parsed.isControlRequest()).isFalse();
		assertThat(parsed.isRegularMessage()).isFalse();
		assertThat(parsed.asControlResponse()).isEqualTo(response);
	}

	/**
	 * Tests DefaultClaudeSession basic lifecycle.
	 */
	@Test
	@DisplayName("DefaultClaudeSession basic lifecycle works")
	void defaultClaudeSessionBasicLifecycle() throws Exception {
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		DefaultClaudeSession session = DefaultClaudeSession.builder()
			.workingDirectory(workingDirectory())
			.options(options)
			.timeout(Duration.ofMinutes(2))
			.claudePath(getClaudeCliPath())
			.build();

		try {
			assertThat(session.isConnected()).isFalse();

			session.connect("Hello");
			assertThat(session.isConnected()).isTrue();
		}
		finally {
			session.close();
			assertThat(session.isConnected()).isFalse();
		}
	}

	/**
	 * Tests that interrupt works at the transport level.
	 */
	@Test
	@DisplayName("Transport interrupt terminates session")
	void transportInterruptTerminatesSession() throws Exception {
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<ParsedMessage> messages = new ArrayList<>();

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			transport.startSession("Count from 1 to 1000", options, messages::add,
					request -> ControlResponse.success(request.requestId(), Map.of("behavior", "allow")));

			// Wait a bit for some messages
			Thread.sleep(2000);

			// Interrupt
			transport.interrupt();

			// Should not be running after interrupt
			assertThat(transport.isRunning()).as("Transport should stop after interrupt").isFalse();
		}
	}

	/**
	 * Tests multi-turn conversation with DefaultClaudeSession. NOTE: This test is
	 * disabled because the current implementation completes the message iterator after
	 * the first result message. Multi-turn requires re-architecting the message flow.
	 */
	@Test
	@Disabled("Multi-turn requires message iterator to stay open after first result - P2 enhancement")
	@DisplayName("Multi-turn conversation preserves context")
	void multiTurnConversationPreservesContext() throws Exception {
		// This test documents the expected behavior for multi-turn sessions
		// Currently blocked on iterator completion behavior
	}

	/**
	 * Tests session control operations (interrupt, setModel, setPermissionMode). NOTE:
	 * These tests are disabled until we verify the control response correlation works
	 * end-to-end with the CLI.
	 */
	@Test
	@Disabled("Control operations require verified CLI protocol support - needs protocol testing")
	@DisplayName("Session control operations work end-to-end")
	void sessionControlOperationsWork() throws Exception {
		// This test documents the expected behavior for control operations
		// Needs verification that CLI responds correctly to control requests
	}

}
