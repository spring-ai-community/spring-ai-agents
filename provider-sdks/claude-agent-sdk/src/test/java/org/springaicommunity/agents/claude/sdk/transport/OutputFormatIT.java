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

import org.springaicommunity.agents.claude.sdk.config.OutputFormat;
import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for different output formats in CLITransport. Tests JSON,
 * STREAM_JSON, and TEXT formats.
 *
 * <p>
 * This test class uses {@link ClaudeCliTestBase} for automatic CLI discovery, eliminating
 * the need for environment variable annotations on each test method.
 * </p>
 */
class OutputFormatIT extends ClaudeCliTestBase {

	private static final Logger logger = LoggerFactory.getLogger(OutputFormatIT.class);

	private CLITransport transport;

	@BeforeEach
	void setUp() {
		transport = new CLITransport(workingDirectory(), Duration.ofMinutes(2), getClaudeCliPath());
		logger.info("Using Claude CLI at: {}", getClaudeCliPath());
	}

	@Test
	void testJsonOutputFormat() throws ClaudeSDKException {
		// Test with JSON output format
		CLIOptions options = CLIOptions.builder()
			.outputFormat(OutputFormat.JSON)
			.timeout(Duration.ofSeconds(30))
			.build();

		List<Message> messages = transport.executeQuery("What is 2+2? Give a brief answer.", options);

		// For JSON format, we expect ResultMessage and AssistantMessage
		assertThat(messages).hasSize(2);

		// First message should be ResultMessage
		Message firstMessage = messages.get(0);
		assertThat(firstMessage).isInstanceOf(ResultMessage.class);

		ResultMessage result = (ResultMessage) firstMessage;

		// Second message should be AssistantMessage
		Message secondMessage = messages.get(1);
		assertThat(secondMessage).isInstanceOf(AssistantMessage.class);
		assertThat(result.result()).isNotBlank();
		assertThat(result.getType()).isEqualTo("result");
		assertThat(result.subtype()).isNotNull();
		assertThat(result.durationMs()).isGreaterThan(0);

		// Verify we have usage and cost information
		if (result.usage() != null) {
			logger.info("JSON format - Usage: {}", result.usage());
		}
		if (result.totalCostUsd() != null) {
			logger.info("JSON format - Total cost: ${}", result.totalCostUsd());
		}

		logger.info("JSON format result: {}", result.result());
	}

	@Test
	void testStreamJsonOutputFormat() throws ClaudeSDKException {
		// Test with STREAM_JSON output format
		CLIOptions options = CLIOptions.builder()
			.outputFormat(OutputFormat.STREAM_JSON)
			.timeout(Duration.ofSeconds(30))
			.build();

		List<Message> messages = transport.executeQuery("What is 3+3? Give a brief answer.", options);

		// Verify we got multiple messages (streaming JSON response)
		assertThat(messages).isNotEmpty();

		// Find the result message
		ResultMessage result = messages.stream()
			.filter(m -> m instanceof ResultMessage)
			.map(m -> (ResultMessage) m)
			.findFirst()
			.orElse(null);

		assertNotNull(result, "Should have a ResultMessage in streaming format");
		assertThat(result.result()).isNotBlank();
		assertThat(result.getType()).isEqualTo("result");

		logger.info("STREAM_JSON format - Got {} messages", messages.size());
		logger.info("STREAM_JSON format result: {}", result.result());
	}

	@Test
	void testTextOutputFormat() throws ClaudeSDKException {
		// Test with TEXT output format
		CLIOptions options = CLIOptions.builder()
			.outputFormat(OutputFormat.TEXT)
			.timeout(Duration.ofSeconds(30))
			.build();

		List<Message> messages = transport.executeQuery("What is 4+4? Give a brief answer.", options);

		// Verify we got exactly one message (text response converted to ResultMessage)
		assertThat(messages).hasSize(1);

		Message message = messages.get(0);
		assertThat(message).isInstanceOf(ResultMessage.class);

		ResultMessage result = (ResultMessage) message;
		assertThat(result.result()).isNotBlank();
		assertThat(result.getType()).isEqualTo("result");
		assertThat(result.subtype()).isEqualTo("success");
		assertThat(result.isError()).isFalse();

		logger.info("TEXT format result: {}", result.result());
	}

	@Test
	void testJsonFormatWithComplexQuery() throws ClaudeSDKException {
		// Test JSON format with a more complex query that might use tools
		CLIOptions options = CLIOptions.builder()
			.outputFormat(OutputFormat.JSON)
			.timeout(Duration.ofSeconds(30))
			.build();

		List<Message> messages = transport.executeQuery("Calculate the square root of 144", options);

		// For JSON format, expect ResultMessage + AssistantMessage
		assertThat(messages).hasSize(2);

		ResultMessage result = (ResultMessage) messages.get(0);
		AssistantMessage assistantMessage = (AssistantMessage) messages.get(1);
		assertThat(result.result()).isNotBlank();
		assertThat(result.result()).containsAnyOf("12", "twelve"); // Expected answer

		// Verify metadata is properly extracted
		assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);

		if (result.usage() != null) {
			logger.info("Complex query usage: {}", result.usage());
		}

		logger.info("Complex query result: {}", result.result());
	}

	@Test
	void testDefaultOutputFormat() throws ClaudeSDKException {
		// Test that default options use JSON format
		CLIOptions defaultOptions = CLIOptions.defaultOptions();

		assertThat(defaultOptions.getOutputFormat()).isEqualTo(OutputFormat.JSON);

		List<Message> messages = transport.executeQuery("Hello", defaultOptions);

		// For JSON format, expect ResultMessage + AssistantMessage
		assertThat(messages).hasSize(2);
		assertThat(messages.get(0)).isInstanceOf(ResultMessage.class);
		assertThat(messages.get(1)).isInstanceOf(AssistantMessage.class);
	}

}