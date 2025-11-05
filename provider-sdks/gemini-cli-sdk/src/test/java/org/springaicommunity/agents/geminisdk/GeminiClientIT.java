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
 * Integration tests for GeminiClient requiring actual Gemini CLI installation.
 *
 * <p>
 * These tests extend {@link BaseGeminiIT} which provides DRY setup for:
 * <ul>
 * <li>CLI availability checking</li>
 * <li>API key validation</li>
 * <li>Automatic test skipping when prerequisites aren't met</li>
 * </ul>
 *
 * <p>
 * Tests will be automatically skipped if Gemini CLI is not available or if required API
 * keys (GEMINI_API_KEY or GOOGLE_API_KEY) are not set.
 */
class GeminiClientIT extends BaseGeminiIT {

	private static final Logger logger = LoggerFactory.getLogger(GeminiClientIT.class);

	private GeminiClient client;

	@BeforeEach
	void setUp() {
		CLIOptions options = CLIOptions.builder()
			.timeout(Duration.ofMinutes(2))
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
	void testCliAvailabilityCheck() throws GeminiSDKException {
		// Test that the client can connect (skip redundant availability check since
		// BaseGeminiIT verified it)
		assertThatNoException().isThrownBy(() -> client.connect(true));

		logger.info("CLI availability check passed - client connected successfully");
	}

	@Test
	void testConnection() throws GeminiSDKException {
		assertThatNoException().isThrownBy(() -> client.connect(false));
		logger.info("Connection test passed");
	}

	@Test
	void testSimpleQuery() throws GeminiSDKException {
		client.connect(false);

		String prompt = "What is 2 + 2? Please provide just the number.";
		String response = client.queryText(prompt);

		assertThat(response).isNotNull().isNotBlank().contains("4"); // Should contain the
																		// answer

		logger.info("Simple query test passed. Response: {}", response.substring(0, Math.min(100, response.length())));
	}

	@Test
	void testDetailedQuery() throws GeminiSDKException {
		client.connect(false);

		String prompt = "Explain what a Java record is in one sentence.";
		QueryResult result = client.query(prompt);

		// Verify result structure
		assertThat(result).isNotNull();
		assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.hasResponse()).isTrue();
		assertThat(result.hasErrors()).isFalse();

		// Verify response content
		String response = result.getResponse().orElse("");
		assertThat(response).isNotBlank().containsIgnoringCase("record"); // Should
																			// mention
																			// "record"

		// Verify metadata
		assertThat(result.metadata()).isNotNull();
		assertThat(result.getFormattedCost()).isNotNull();

		logger.info("Detailed query test passed:");
		logger.info("  Response length: {}", response.length());
		logger.info("  Cost: {}", result.getFormattedCost());
		logger.info("  Efficiency: {}", result.getEfficiencySummary());
	}

	@Test
	void testCustomConfiguration() throws GeminiSDKException {
		// Test with flash model specifically
		CLIOptions flashOptions = CLIOptions.builder()
			.model("gemini-2.5-flash")
			.yoloMode(true)
			.timeout(Duration.ofMinutes(1))
			.build();

		try (GeminiClient flashClient = GeminiClient.create(flashOptions)) {
			flashClient.connect(true);

			String prompt = "Say 'Hello from Gemini Flash!' and nothing else.";
			QueryResult result = flashClient.query(prompt, flashOptions);

			assertThat(result.isSuccessful()).isTrue();
			assertThat(result.metadata().model()).contains("flash");

			String response = result.getResponse().orElse("");
			assertThat(response).isNotBlank().containsIgnoringCase("hello");

			logger.info("Custom configuration test passed with model: {}", result.metadata().model());
		}
	}

	@Test
	void testErrorHandling() throws GeminiSDKException {
		client.connect(true);

		// Test with an intentionally problematic prompt
		String prompt = ""; // Empty prompt should be handled gracefully

		assertThatThrownBy(() -> client.query(prompt)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("cannot be null or empty");

		logger.info("Error handling test passed");
	}

}