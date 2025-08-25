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

import org.springaicommunity.agents.geminisdk.transport.CliAvailabilityResult;
import org.springaicommunity.agents.geminisdk.util.GeminiCliDiscovery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all Gemini integration tests.
 *
 * <p>
 * This class provides DRY setup and validation for integration tests that require:
 * <ul>
 * <li>Gemini CLI availability</li>
 * <li>Required API keys (GEMINI_API_KEY or GOOGLE_API_KEY)</li>
 * </ul>
 *
 * <p>
 * All integration test classes should extend this base class instead of duplicating the
 * setup logic. The CLI availability check is performed once in {@code @BeforeAll} to
 * avoid redundant process calls.
 *
 * <p>
 * Tests will be automatically skipped if prerequisites are not met.
 *
 * @see GeminiClientIT
 */
@EnabledIf("org.springaicommunity.agents.geminisdk.BaseGeminiIT#canRunIntegrationTests")
public abstract class BaseGeminiIT {

	private static final Logger logger = LoggerFactory.getLogger(BaseGeminiIT.class);

	// Static fields to cache availability checks across all test methods
	private static boolean cliAvailable = false;

	private static boolean apiKeyAvailable = false;

	private static boolean setupCompleted = false;

	/**
	 * Performs one-time setup for all integration tests. Checks CLI availability and API
	 * key presence.
	 */
	@BeforeAll
	static void setUpIntegrationTestPrerequisites() {
		if (setupCompleted) {
			return; // Avoid duplicate setup if called multiple times
		}

		logger.info("Setting up integration test prerequisites...");

		// Check CLI availability once
		cliAvailable = checkCliAvailability();

		// Check API key availability once
		apiKeyAvailable = checkApiKeyAvailability();

		setupCompleted = true;

		if (cliAvailable && apiKeyAvailable) {
			logger.info("✅ Integration test prerequisites met - tests will run");
		}
		else {
			logger.warn("❌ Integration test prerequisites not met - tests will be skipped");
			if (!cliAvailable) {
				logger.warn("   - Gemini CLI not available");
			}
			if (!apiKeyAvailable) {
				logger.warn("   - API key not found (set GEMINI_API_KEY or GOOGLE_API_KEY)");
			}
		}
	}

	/**
	 * Checks if Gemini CLI is available for testing. This method is called once during
	 * setup to avoid redundant process calls.
	 */
	private static boolean checkCliAvailability() {
		// Use findGeminiCommand once and check that specific command
		String geminiCommand = GeminiCliDiscovery.findGeminiCommand();
		CliAvailabilityResult result = GeminiCliDiscovery.checkCommandAvailability(geminiCommand);

		if (result.isAvailable()) {
			logger.info("Gemini CLI is available for integration testing: {}",
					result.getVersion().orElse("unknown version"));
			return true;
		}
		else {
			logger.warn("Gemini CLI is not available for integration testing: {}",
					result.getReason().orElse("unknown reason"));
			return false;
		}
	}

	/**
	 * Checks if required API keys are available for Gemini CLI authentication. Gemini CLI
	 * requires either GEMINI_API_KEY or GOOGLE_API_KEY environment variable.
	 */
	private static boolean checkApiKeyAvailability() {
		String geminiApiKey = System.getenv("GEMINI_API_KEY");
		String googleApiKey = System.getenv("GOOGLE_API_KEY");

		boolean hasApiKey = (geminiApiKey != null && !geminiApiKey.trim().isEmpty())
				|| (googleApiKey != null && !googleApiKey.trim().isEmpty());

		if (hasApiKey) {
			logger.info("API key found for Gemini CLI authentication");
		}
		else {
			logger.warn(
					"No API key found. Set GEMINI_API_KEY or GOOGLE_API_KEY environment variable for integration testing");
		}
		return hasApiKey;
	}

	/**
	 * Static method for @EnabledIf condition. This method is called by JUnit to determine
	 * if tests should run.
	 * @return true if both CLI and API key are available
	 */
	public static boolean canRunIntegrationTests() {
		// Ensure setup has run (in case @EnabledIf is evaluated before @BeforeAll)
		if (!setupCompleted) {
			setUpIntegrationTestPrerequisites();
		}
		return cliAvailable && apiKeyAvailable;
	}

	/**
	 * Gets the CLI availability status for use by subclasses.
	 * @return true if CLI is available
	 */
	protected static boolean isCliAvailable() {
		return cliAvailable;
	}

	/**
	 * Gets the API key availability status for use by subclasses.
	 * @return true if API key is available
	 */
	protected static boolean isApiKeyAvailable() {
		return apiKeyAvailable;
	}

	/**
	 * Resets the setup state - useful for testing the base class itself.
	 */
	static void resetSetup() {
		setupCompleted = false;
		cliAvailable = false;
		apiKeyAvailable = false;
	}

}