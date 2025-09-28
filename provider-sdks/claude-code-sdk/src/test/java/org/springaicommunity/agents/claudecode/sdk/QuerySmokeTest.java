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

package org.springaicommunity.agents.claudecode.sdk;

import org.springaicommunity.agents.claudecode.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claudecode.sdk.types.QueryResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Query functionality.
 *
 * <p>
 * This test extends {@link ClaudeCliTestBase} which automatically discovers Claude CLI
 * and ensures all tests fail gracefully with a clear message if Claude CLI is not
 * available.
 * </p>
 */
class QuerySmokeTest extends ClaudeCliTestBase {

	@Test
	void testBasicQuery() throws Exception {
		QueryResult result = Query.execute("What is 1+1?");

		assertThat(result).isNotNull();
		assertThat(result.messages()).isNotEmpty();
		assertThat(result.metadata()).isNotNull();
		assertThat(result.isSuccessful()).isTrue();
	}

	@Test
	void testQueryWithOptions() throws Exception {
		CLIOptions options = CLIOptions.builder()
			.timeout(Duration.ofMinutes(2))
			.systemPrompt("You are a helpful math tutor.")
			.build();

		QueryResult result = Query.execute("What is 2+2?", options);

		assertThat(result).isNotNull();
		assertThat(result.messages()).isNotEmpty();
		assertThat(result.metadata()).isNotNull();
	}

	@Test
	void testQueryResultAnalysis() throws Exception {
		QueryResult result = Query.execute("Hello, world!");

		// Test domain-specific methods
		assertThat(result.getMessageCount()).isGreaterThan(0);
		assertThat(result.getFirstAssistantResponse()).isPresent();

		// Test metadata analysis
		assertThat(result.metadata().model()).isNotNull();
		assertThat(result.metadata().getDuration()).isNotNull();
	}

	@Test
	void testClaudeCliSanityCheck() throws Exception {
		// Direct zt-exec test to reproduce manual bash test and debug timeouts
		String claudePath = getClaudeCliPath();

		// Test the exact command that was timing out in CI
		org.zeroturnaround.exec.ProcessExecutor executor = new org.zeroturnaround.exec.ProcessExecutor()
			.command(claudePath, "--print", "--output-format", "json", "--append-system-prompt",
					"You are a helpful math tutor.", "--permission-mode", "bypassPermissions", "--", "What is 2+2?")
			.timeout(2, java.util.concurrent.TimeUnit.MINUTES) // AgentClient adds
																// overhead, need 2m
			.readOutput(true);

		// Add API key if available
		String apiKey = System.getenv("ANTHROPIC_API_KEY");
		if (apiKey != null && !apiKey.trim().isEmpty()) {
			executor.environment("ANTHROPIC_API_KEY", apiKey);
			executor.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java");
		}

		long startTime = System.currentTimeMillis();
		org.zeroturnaround.exec.ProcessResult result = executor.execute();
		long duration = System.currentTimeMillis() - startTime;

		// Verify the command completed successfully
		assertThat(result.getExitValue()).isEqualTo(0);
		assertThat(result.outputUTF8()).isNotEmpty();

		// Log timing information for debugging
		System.out.printf("Claude CLI sanity check completed in %d ms%n", duration);
		System.out.printf("Output length: %d characters%n", result.outputUTF8().length());

		// Verify we got JSON output (should contain "result" field)
		assertThat(result.outputUTF8()).contains("\"result\"");
	}

}