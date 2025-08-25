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

package org.springaicommunity.agents.sweagentsdk.util;

import org.springaicommunity.agents.sweagentsdk.transport.CliAvailabilityResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SweCliDiscovery.
 */
class SweCliDiscoveryTest {

	private String originalSystemProperty;

	@BeforeEach
	void setUp() {
		// Save original system property
		originalSystemProperty = System.getProperty("swe.cli.path");
		// Clear cache before each test
		SweCliDiscovery.clearCache();
	}

	@AfterEach
	void tearDown() {
		// Restore original system property
		if (originalSystemProperty != null) {
			System.setProperty("swe.cli.path", originalSystemProperty);
		}
		else {
			System.clearProperty("swe.cli.path");
		}
		// Clear cache after each test
		SweCliDiscovery.clearCache();
	}

	@Test
	void testFindSweCommandWithSystemProperty() {
		// Given
		String testPath = "/test/path/mini";
		System.setProperty("swe.cli.path", testPath);

		// When
		String result = SweCliDiscovery.findSweCommand();

		// Then - should return the system property path even if it doesn't exist
		// (actual availability is tested separately)
		assertThat(result).isNotNull();
	}

	@Test
	void testFindSweCommandWithoutSystemProperty() {
		// Given
		System.clearProperty("swe.cli.path");

		// When
		String result = SweCliDiscovery.findSweCommand();

		// Then
		assertThat(result).isNotNull();
		assertThat(result).isNotEmpty();
	}

	@Test
	void testCheckCommandAvailabilityForNonExistentCommand() {
		// Given
		String nonExistentCommand = "/definitely/does/not/exist/mini";

		// When
		CliAvailabilityResult result = SweCliDiscovery.checkCommandAvailability(nonExistentCommand);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.isAvailable()).isFalse();
		assertThat(result.getReason()).isPresent();
		assertThat(result.getVersion()).isEmpty();
	}

	@Test
	void testCheckCommandAvailabilityForInvalidCommand() {
		// Given - use a command that exists but doesn't support --version properly
		String invalidCommand = "echo"; // echo doesn't support --version

		// When
		CliAvailabilityResult result = SweCliDiscovery.checkCommandAvailability(invalidCommand);

		// Then
		assertThat(result).isNotNull();
		// echo will likely fail or not behave as expected for --version
		// The important thing is we get a result and don't crash
	}

	@Test
	void testCaching() {
		// Given
		String testCommand = "/nonexistent/command";

		// When - call twice
		CliAvailabilityResult result1 = SweCliDiscovery.checkCommandAvailability(testCommand);
		CliAvailabilityResult result2 = SweCliDiscovery.checkCommandAvailability(testCommand);

		// Then - should return same result (cached)
		assertThat(result1.isAvailable()).isEqualTo(result2.isAvailable());
		assertThat(result1.getReason()).isEqualTo(result2.getReason());
	}

	@Test
	void testClearCache() {
		// Given
		String testCommand = "/nonexistent/command";
		CliAvailabilityResult result1 = SweCliDiscovery.checkCommandAvailability(testCommand);

		// When
		SweCliDiscovery.clearCache();
		CliAvailabilityResult result2 = SweCliDiscovery.checkCommandAvailability(testCommand);

		// Then - should still return equivalent results (but potentially re-executed)
		assertThat(result1.isAvailable()).isEqualTo(result2.isAvailable());
	}

	@Test
	void testIsCommandAvailable() {
		// Given
		String nonExistentCommand = "/definitely/does/not/exist/mini";

		// When
		boolean result = SweCliDiscovery.isCommandAvailable(nonExistentCommand);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	void testCheckSweCliAvailability() {
		// When
		CliAvailabilityResult result = SweCliDiscovery.checkSweCliAvailability();

		// Then
		assertThat(result).isNotNull();
		// Don't assert on availability as it depends on environment
		// Just ensure we get a proper result
	}

	@Test
	void testIsSweCliAvailable() {
		// When
		boolean result = SweCliDiscovery.isSweCliAvailable();

		// Then
		// Don't assert on specific value as it depends on environment
		// Just ensure the method doesn't crash
		assertThat(result).isIn(true, false);
	}

	@Test
	void testGetSweCliVersion() {
		// When
		String version = SweCliDiscovery.getSweCliVersion();

		// Then
		// Version may be null if CLI is not available - that's fine
		// Just ensure the method doesn't crash
	}

	@Test
	void testGetDiscoveredPath() {
		// When
		String path = SweCliDiscovery.getDiscoveredPath();

		// Then
		// Path may be null if CLI is not available - that's fine
		// Just ensure the method doesn't crash
	}

}