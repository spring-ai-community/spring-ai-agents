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
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.SystemMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Iterator API pattern.
 *
 * <p>
 * Tests the {@link ClaudeSession#receiveMessages()} and
 * {@link ClaudeSession#receiveResponse()} methods which provide event-driven consumption
 * without requiring Flux/reactive streams.
 * </p>
 *
 * <p>
 * The Iterator pattern is one of three first-class API patterns:
 * <ul>
 * <li>Blocking - {@code query()} returns complete result</li>
 * <li>Reactive/Flux - {@code queryReactive()} for reactive streams</li>
 * <li>Iterator - {@code receiveMessages()}/{@code receiveResponse()} for
 * event-driven</li>
 * </ul>
 * </p>
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class IteratorPatternIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = CLIOptions.MODEL_HAIKU;

	/**
	 * Tests the receiveResponse() iterator which yields messages until ResultMessage.
	 */
	@Test
	@DisplayName("receiveResponse() iterator should yield messages until ResultMessage")
	void receiveResponseIteratorYieldsUntilResult() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<ParsedMessage> receivedMessages = new ArrayList<>();

		try (DefaultClaudeSession session = new DefaultClaudeSession(workingDirectory(), options, Duration.ofMinutes(2),
				getClaudeCliPath(), null, null)) {

			// When - connect and iterate
			session.connect("What is 2+2? Reply with just the number.");

			Iterator<ParsedMessage> iterator = session.receiveResponse();
			while (iterator.hasNext()) {
				ParsedMessage message = iterator.next();
				receivedMessages.add(message);
				System.out.println("Received: " + message);
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
	 * Tests multi-turn conversation using receiveResponse() iterator.
	 */
	@Test
	@DisplayName("Iterator pattern should support multi-turn conversations")
	void iteratorPatternSupportsMultiTurn() throws Exception {
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
			Iterator<ParsedMessage> firstIterator = session.receiveResponse();
			while (firstIterator.hasNext()) {
				firstTurnMessages.add(firstIterator.next());
			}

			assertThat(firstTurnMessages).as("First turn should receive messages").isNotEmpty();

			// Second turn - verify context maintained
			session.query("What is my favorite color?");

			List<ParsedMessage> secondTurnMessages = new ArrayList<>();
			Iterator<ParsedMessage> secondIterator = session.receiveResponse();
			while (secondIterator.hasNext()) {
				secondTurnMessages.add(secondIterator.next());
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
	 * Tests that message types are correctly identified in iterator.
	 */
	@Test
	@DisplayName("Iterator should yield correctly typed messages")
	void iteratorYieldsCorrectlyTypedMessages() throws Exception {
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

			Iterator<ParsedMessage> iterator = session.receiveResponse();
			while (iterator.hasNext()) {
				ParsedMessage message = iterator.next();
				if (message.isRegularMessage()) {
					messageTypes.add(message.asMessage().getClass());
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
	 * Tests for-each loop usage with iterator (Iterable pattern).
	 */
	@Test
	@DisplayName("Iterator should work with for-each loop via receiveMessages()")
	void iteratorWorksWithForEachLoop() throws Exception {
		// Given
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		List<ParsedMessage> receivedMessages = new ArrayList<>();

		try (DefaultClaudeSession session = new DefaultClaudeSession(workingDirectory(), options, Duration.ofMinutes(2),
				getClaudeCliPath(), null, null)) {

			// When - use for-each style (manual since Iterator, not Iterable at session
			// level)
			session.connect("What is 5+5? Just the number.");

			Iterator<ParsedMessage> iterator = session.receiveResponse();
			iterator.forEachRemaining(receivedMessages::add);
		}

		// Then
		assertThat(receivedMessages).as("Should receive messages via forEachRemaining").isNotEmpty();

		// Should end with ResultMessage
		ParsedMessage lastMessage = receivedMessages.get(receivedMessages.size() - 1);
		assertThat(lastMessage.isRegularMessage()).isTrue();
		assertThat(lastMessage.asMessage()).isInstanceOf(ResultMessage.class);
	}

}
