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

package org.springaicommunity.agents.claude.sdk;

import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.QueryResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for ClaudeAgentClient functionality.
 *
 * <p>
 * This test extends {@link ClaudeCliTestBase} which automatically discovers Claude CLI
 * and ensures all tests fail gracefully with a clear message if Claude CLI is not
 * available.
 * </p>
 */
class ClaudeAgentClientIT extends ClaudeCliTestBase {

	@Test
	void testClientConnection() throws Exception {
		try (ClaudeAgentClient client = ClaudeAgentClient.create(CLIOptions.defaultOptions(), workingDirectory(),
				getClaudeCliPath())) {
			client.connect();
			assertThat(client.isConnected()).isTrue();
		}
	}

	@Test
	void testClientQuery() throws Exception {
		try (ClaudeAgentClient client = ClaudeAgentClient.create(CLIOptions.defaultOptions(), workingDirectory(),
				getClaudeCliPath())) {
			client.connect();

			QueryResult result = client.query("What is 3+3?");

			assertThat(result).isNotNull();
			assertThat(result.messages()).isNotEmpty();
			assertThat(result.isSuccessful()).isTrue();
		}
	}

	@Test
	void testClientStreamingQuery() throws Exception {
		try (ClaudeAgentClient client = ClaudeAgentClient.create(CLIOptions.defaultOptions(), workingDirectory(),
				getClaudeCliPath())) {
			client.connect();

			List<Message> receivedMessages = new ArrayList<>();

			client.queryStreaming("Tell me a joke", receivedMessages::add);

			assertThat(receivedMessages).isNotEmpty();
		}
	}

	@Test
	void testClientWithOptions() throws Exception {
		CLIOptions options = CLIOptions.builder()
			.timeout(Duration.ofSeconds(45))
			.systemPrompt("You are a helpful assistant.")
			.build();

		try (ClaudeAgentClient client = ClaudeAgentClient.create(options)) {
			client.connect();

			QueryResult result = client.query("What is the capital of France?");

			assertThat(result).isNotNull();
			assertThat(result.getFirstAssistantResponse()).isPresent();
		}
	}

}