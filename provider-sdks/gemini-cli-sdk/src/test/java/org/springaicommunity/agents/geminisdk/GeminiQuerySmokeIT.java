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

package org.springaicommunity.agents.geminisdk;

import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.geminisdk.types.QueryResult;
import org.springaicommunity.agents.geminisdk.types.ResultStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Lightweight integration tests for basic Gemini query functionality.
 *
 * <p>
 * This test extends {@link BaseGeminiIT} which automatically discovers Gemini CLI and
 * ensures all tests fail gracefully with a clear message if Gemini CLI is not available
 * or API keys are missing.
 * </p>
 *
 * <p>
 * This is the Gemini equivalent of {@code QuerySmokeTest.java} for Claude Code SDK,
 * containing only the 3 most basic integration tests for smoke testing.
 * </p>
 */
class GeminiQuerySmokeIT extends BaseGeminiIT {

	private static final Logger logger = LoggerFactory.getLogger(GeminiQuerySmokeIT.class);

	private GeminiClient client;

	@BeforeEach
	void setUp() {
		CLIOptions options = CLIOptions.builder()
			.timeout(Duration.ofMinutes(1))
			.yoloMode(true) // Enable non-interactive mode
			.build();

		try {
			client = GeminiClient.create(options);
		}
		catch (GeminiSDKException e) {
			logger.warn("Failed to create GeminiClient: {}", e.getMessage());
		}
	}

	@Test
	void testBasicQuery() throws GeminiSDKException {
		client.connect(true);

		String prompt = "What is 1+1?";
		String response = client.queryText(prompt);

		assertThat(response).isNotNull().isNotBlank();
		// Should contain the answer "2" somewhere
		assertThat(response).containsAnyOf("2", "two", "Two");
	}

	@Test
	void testQueryWithOptions() throws GeminiSDKException {
		client.connect(true);

		CLIOptions options = CLIOptions.builder()
			.model("gemini-pro")
			.timeout(Duration.ofSeconds(30))
			.yoloMode(true)
			.build();

		QueryResult result = client.query("What is 2+2?", options);

		assertThat(result).isNotNull();
		assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.hasResponse()).isTrue();
		// Should contain the answer "4" somewhere
		assertThat(result.getResponse().orElse("")).containsAnyOf("4", "four", "Four");
	}

	@Test
	void testQueryResultAnalysis() throws GeminiSDKException {
		client.connect(true);

		QueryResult result = client.query("Hello, world!");

		// Test basic result structure
		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.hasResponse()).isTrue();
		assertThat(result.hasErrors()).isFalse();

		// Test metadata analysis
		assertThat(result.metadata()).isNotNull();
		assertThat(result.getFormattedCost()).isNotNull();
		assertThat(result.getEfficiencySummary()).isNotNull();
	}

}