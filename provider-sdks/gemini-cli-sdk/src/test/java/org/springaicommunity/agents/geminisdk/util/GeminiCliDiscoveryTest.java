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

package org.springaicommunity.agents.geminisdk.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GeminiCliDiscovery utility class.
 *
 * @author Mark Pollack
 */
class GeminiCliDiscoveryTest {

	@BeforeEach
	void clearCache() {
		// Clear any cached results before each test
		GeminiCliDiscovery.clearCache();
	}

	@Test
	void testFindGeminiCommandBasic() {
		String command = GeminiCliDiscovery.findGeminiCommand();
		assertThat(command).isNotNull();
		// Should at least return "gemini" as fallback
		assertThat(command).containsIgnoringCase("gemini");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
	void testGeminiCliAvailability() {
		boolean isAvailable = GeminiCliDiscovery.isGeminiCliAvailable();
		System.out.println("Gemini CLI available: " + isAvailable);

		String discoveredPath = GeminiCliDiscovery.getDiscoveredPath();
		System.out.println("Discovered path: " + discoveredPath);

		if (isAvailable) {
			assertThat(discoveredPath).isNotNull();
			String version = GeminiCliDiscovery.getGeminiCliVersion();
			System.out.println("CLI version: " + version);
			assertThat(version).isNotNull();
		}
		else {
			System.out.println("Gemini CLI not available - this may be expected in CI environments");
		}
	}

	@Test
	void testExtractNvmNodePath() {
		String nvmPath = "/home/user/.nvm/versions/node/v22.15.0/bin/gemini";
		String extractedPath = GeminiCliDiscovery.extractNvmNodePath(nvmPath);
		assertThat(extractedPath).isEqualTo("/home/user/.nvm/versions/node/v22.15.0/bin");

		String nonNvmPath = "/usr/local/bin/gemini";
		String extractedNonNvmPath = GeminiCliDiscovery.extractNvmNodePath(nonNvmPath);
		assertThat(extractedNonNvmPath).isNull();
	}

	@Test
	void testGetGeminiCommand() {
		// Test regular command
		String[] command = GeminiCliDiscovery.getGeminiCommand("gemini", "--help");
		assertThat(command).containsExactly("gemini", "--help");

		// Test nvm command
		String nvmPath = "/home/user/.nvm/versions/node/v22.15.0/bin/gemini";
		String[] nvmCommand = GeminiCliDiscovery.getGeminiCommand(nvmPath, "--version");
		assertThat(nvmCommand).containsExactly("/home/user/.nvm/versions/node/v22.15.0/bin/node",
				"/home/user/.nvm/versions/node/v22.15.0/bin/gemini", "--version");
	}

	@Test
	void testCommandAvailabilityForNonExistentCommand() {
		boolean available = GeminiCliDiscovery.isCommandAvailable("/nonexistent/command");
		assertThat(available).isFalse();
	}

	@Test
	void testCacheClearing() {
		// This test mainly verifies that clearCache doesn't throw exceptions
		GeminiCliDiscovery.clearCache();
		// Call a method to populate cache
		GeminiCliDiscovery.findGeminiCommand();
		// Clear again
		GeminiCliDiscovery.clearCache();
		// Should work fine
		assertThat(GeminiCliDiscovery.findGeminiCommand()).isNotNull();
	}

}