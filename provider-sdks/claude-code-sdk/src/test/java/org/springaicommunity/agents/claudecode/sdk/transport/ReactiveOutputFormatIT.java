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

package org.springaicommunity.agents.claudecode.sdk.transport;

import org.springaicommunity.agents.claudecode.sdk.config.OutputFormat;
import org.springaicommunity.agents.claudecode.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claudecode.sdk.types.Message;
import org.springaicommunity.agents.claudecode.sdk.types.ResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.file.Paths;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ReactiveTransport with different output formats.
 */
class ReactiveOutputFormatIT extends ClaudeCliTestBase {

	private static final Logger logger = LoggerFactory.getLogger(ReactiveOutputFormatIT.class);

	private ReactiveTransport reactiveTransport;

	@BeforeEach
	void setUp() {
		reactiveTransport = new ReactiveTransport(Paths.get("."), Duration.ofMinutes(2), getClaudeCliPath());
	}

	@Test
	void testReactiveWithStreamJsonFormat() {
		CLIOptions options = CLIOptions.builder()
			.outputFormat(OutputFormat.STREAM_JSON)
			.timeout(Duration.ofSeconds(30))
			.build();

		Flux<Message> messages = reactiveTransport.executeReactiveQuery("What is 5+5?", options);

		StepVerifier.create(messages).expectNextMatches(message -> {
			logger.info("Received message: {}", message.getClass().getSimpleName());
			return message instanceof Message; // First message (SystemMessage)
		}).expectNextMatches(message -> {
			logger.info("Received message: {}", message.getClass().getSimpleName());
			return message instanceof Message; // Second message (AssistantMessage)
		}).thenConsumeWhile(message -> {
			logger.info("Consuming message: {}", message.getClass().getSimpleName());
			return message instanceof Message; // Any additional messages
		}).expectComplete().verify(Duration.ofSeconds(30));
	}

	@Test
	void testReactiveWithJsonFormat() {
		CLIOptions options = CLIOptions.builder()
			.outputFormat(OutputFormat.JSON)
			.timeout(Duration.ofSeconds(30))
			.build();

		Flux<Message> messages = reactiveTransport.executeReactiveQuery("What is 7+7?", options);

		StepVerifier.create(messages).expectNextMatches(message -> {
			logger.info("Received message: {}", message.getClass().getSimpleName());
			return message instanceof Message;
		}).thenConsumeWhile(message -> {
			logger.info("Consuming message: {}", message.getClass().getSimpleName());
			return message instanceof Message;
		}).expectComplete().verify(Duration.ofSeconds(30));
	}

	@Test
	void testReactiveWithTextFormat() {
		// This should automatically convert to STREAM_JSON with warning
		CLIOptions options = CLIOptions.builder()
			.outputFormat(OutputFormat.TEXT)
			.timeout(Duration.ofSeconds(30))
			.build();

		Flux<Message> messages = reactiveTransport.executeReactiveQuery("What is 9+9?", options);

		StepVerifier.create(messages).expectNextMatches(message -> {
			logger.info("Received message: {}", message.getClass().getSimpleName());
			return message instanceof Message;
		}).thenConsumeWhile(message -> {
			logger.info("Consuming message: {}", message.getClass().getSimpleName());
			return message instanceof Message;
		}).expectComplete().verify(Duration.ofSeconds(30));
	}

}