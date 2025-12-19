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
import org.junit.jupiter.api.Timeout;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.streaming.MessageReceiver;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.SystemMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the POC MessageReceiver API pattern.
 *
 * <p>
 * Tests the {@link ClaudeSession#messageReceiver()} and
 * {@link ClaudeSession#responseReceiver()} methods which provide event-driven consumption
 * using the simpler null-at-end pattern (based on Spring RestClient streaming POC).
 * </p>
 *
 * <p>
 * The MessageReceiver pattern is one of three first-class API patterns:
 * <ul>
 * <li>Blocking - {@code query()} returns complete result</li>
 * <li>Reactive/Flux - {@code queryReactive()} for reactive streams</li>
 * <li>Iterator - {@code receiveMessages()}/{@code receiveResponse()} for
 * event-driven</li>
 * <li>MessageReceiver - {@code messageReceiver()}/{@code responseReceiver()} for simpler
 * null-at-end semantics</li>
 * </ul>
 * </p>
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class MessageReceiverPatternIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = CLIOptions.MODEL_HAIKU;

	/**
	 * Tests the responseReceiver() which yields messages until ResultMessage.
	 */
	@Test
	@DisplayName("responseReceiver() should yield messages until ResultMessage then null")
	void responseReceiverYieldsUntilResult() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<ParsedMessage> receivedMessages = new ArrayList<>();

		try (DefaultClaudeSession session = new DefaultClaudeSession(workingDirectory(), options, Duration.ofMinutes(2),
				getClaudeCliPath(), null, null)) {

			// When - connect and receive
			session.connect("What is 2+2? Reply with just the number.");

			try (MessageReceiver receiver = session.responseReceiver()) {
				ParsedMessage msg;
				while ((msg = receiver.next()) != null) {
					receivedMessages.add(msg);
					System.out.println("Received: " + msg);
				}
			}
		}

		// Then
		assertThat(receivedMessages).as("Should receive at least one message").isNotEmpty();

		// Should end with a ResultMessage
		ParsedMessage lastMessage = receivedMessages.get(receivedMessages.size() - 1);
		assertThat(lastMessage.isRegularMessage()).isTrue();
		assertThat(lastMessage.asMessage()).isInstanceOf(ResultMessage.class);

		// Should contain result with answer
		ResultMessage result = (ResultMessage) lastMessage.asMessage();
		assertThat(result.result()).as("Result should contain answer").isNotNull();
	}

	/**
	 * Tests multi-turn conversation using responseReceiver().
	 */
	@Test
	@DisplayName("MessageReceiver pattern should support multi-turn conversations")
	void messageReceiverSupportsMultiTurn() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		try (DefaultClaudeSession session = new DefaultClaudeSession(workingDirectory(), options, Duration.ofMinutes(2),
				getClaudeCliPath(), null, null)) {

			// First turn - establish context
			session.connect("My favorite color is blue. Remember this.");

			List<ParsedMessage> firstTurnMessages = new ArrayList<>();
			try (MessageReceiver receiver = session.responseReceiver()) {
				ParsedMessage msg;
				while ((msg = receiver.next()) != null) {
					firstTurnMessages.add(msg);
				}
			}

			assertThat(firstTurnMessages).as("First turn should receive messages").isNotEmpty();

			// Second turn - verify context maintained
			session.query("What is my favorite color?");

			List<ParsedMessage> secondTurnMessages = new ArrayList<>();
			try (MessageReceiver receiver = session.responseReceiver()) {
				ParsedMessage msg;
				while ((msg = receiver.next()) != null) {
					secondTurnMessages.add(msg);
				}
			}

			assertThat(secondTurnMessages).as("Second turn should receive messages").isNotEmpty();

			// Verify the response mentions blue
			ParsedMessage lastMessage = secondTurnMessages.get(secondTurnMessages.size() - 1);
			assertThat(lastMessage.isRegularMessage()).isTrue();
			ResultMessage result = (ResultMessage) lastMessage.asMessage();
			assertThat(result.result().toLowerCase()).as("Response should mention blue").contains("blue");
		}
	}

	/**
	 * Tests that message types are correctly identified.
	 */
	@Test
	@DisplayName("MessageReceiver should yield correctly typed messages")
	void messageReceiverYieldsCorrectlyTypedMessages() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<Class<? extends Message>> messageTypes = new ArrayList<>();

		try (DefaultClaudeSession session = new DefaultClaudeSession(workingDirectory(), options, Duration.ofMinutes(2),
				getClaudeCliPath(), null, null)) {

			// When
			session.connect("Say 'hello' in one word.");

			try (MessageReceiver receiver = session.responseReceiver()) {
				ParsedMessage msg;
				while ((msg = receiver.next()) != null) {
					if (msg.isRegularMessage()) {
						messageTypes.add(msg.asMessage().getClass());
					}
				}
			}
		}

		// Then - should have proper message type sequence
		assertThat(messageTypes).as("Should have message types").isNotEmpty();

		// First message is typically SystemMessage
		if (messageTypes.size() > 1) {
			assertThat(messageTypes.get(0)).as("First message should be SystemMessage").isEqualTo(SystemMessage.class);
		}

		// Last message should be ResultMessage
		assertThat(messageTypes.get(messageTypes.size() - 1)).as("Last message should be ResultMessage")
			.isEqualTo(ResultMessage.class);

		// Should contain AssistantMessage
		assertThat(messageTypes).as("Should contain AssistantMessage").contains(AssistantMessage.class);
	}

	/**
	 * Tests that MessageReceiver properly cleans up on close.
	 */
	@Test
	@DisplayName("MessageReceiver should be safe to close at any point")
	void messageReceiverSafeToCloseEarly() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		int messageCount = 0;

		try (DefaultClaudeSession session = new DefaultClaudeSession(workingDirectory(), options, Duration.ofMinutes(2),
				getClaudeCliPath(), null, null)) {

			session.connect("What is 2+2? Reply with just the number.");

			// Consume only one message then close receiver early
			try (MessageReceiver receiver = session.responseReceiver()) {
				ParsedMessage msg = receiver.next();
				if (msg != null) {
					messageCount++;
					System.out.println("Got first message: " + msg);
				}
				// Close early - should be safe
			}
		}

		// Then - should have received at least one message
		assertThat(messageCount).as("Should receive at least one message").isGreaterThanOrEqualTo(1);
	}

}
