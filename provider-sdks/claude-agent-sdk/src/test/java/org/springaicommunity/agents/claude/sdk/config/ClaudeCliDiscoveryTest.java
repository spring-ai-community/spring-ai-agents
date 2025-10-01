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

package org.springaicommunity.agents.claude.sdk.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ClaudeCliDiscovery utility class.
 *
 * @author Mark Pollack
 */
class ClaudeCliDiscoveryTest {

	@Test
	@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
	void testGetDiscoveredPath() {
		String discoveredPath = ClaudeCliDiscovery.getDiscoveredPath();
		System.out.println("Claude CLI discovered path: " + discoveredPath);

		if (discoveredPath != null) {
			// getDiscoveredPath() should always return a full, absolute path
			Path cliPath = Path.of(discoveredPath);
			assertThat(Files.exists(cliPath)).isTrue();
			assertThat(Files.isExecutable(cliPath)).isTrue();
			// Ensure it's an absolute path
			assertThat(cliPath.isAbsolute()).isTrue();
		}
		else {
			System.out.println("Claude CLI not found - this may be expected in some environments");
		}
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
	void testIsClaudeCliAvailable() {
		// Test if Claude CLI is available
		boolean isAvailable = ClaudeCliDiscovery.isClaudeCliAvailable();
		System.out.println("Claude CLI is available: " + isAvailable);

		if (isAvailable) {
			// If available, getDiscoveredPath should not return null
			String path = ClaudeCliDiscovery.getDiscoveredPath();
			assertThat(path).isNotNull();
			System.out.println("Claude CLI path: " + path);
		}
	}

	@Test
	void testGetDiscoveredPathWhenNotAvailable() {
		// Clear any system properties that might help discovery
		String originalPath = System.getProperty("claude.cli.path");
		try {
			System.clearProperty("claude.cli.path");
			// Force rediscovery to clear any cached results
			ClaudeCliDiscovery.forceRediscovery();

			// If Claude is truly not available, this should return null
			// If it's available, that's fine too - we're just testing the method works
			String path = ClaudeCliDiscovery.getDiscoveredPath();
			// Should not throw - just return null if not found
		}
		finally {
			if (originalPath != null) {
				System.setProperty("claude.cli.path", originalPath);
			}
			// Force rediscovery to restore normal state
			ClaudeCliDiscovery.forceRediscovery();
		}
	}

	@Test
	void testDiscoverClaudePathWhenNotAvailable() {
		// Test the exception-throwing discovery method
		String originalPath = System.getProperty("claude.cli.path");
		try {
			System.setProperty("claude.cli.path", "/nonexistent/claude");
			ClaudeCliDiscovery.forceRediscovery();

			assertThatThrownBy(() -> ClaudeCliDiscovery.discoverClaudePath())
				.isInstanceOf(ClaudeCliDiscovery.ClaudeCliNotFoundException.class);
		}
		finally {
			if (originalPath != null) {
				System.setProperty("claude.cli.path", originalPath);
			}
			else {
				System.clearProperty("claude.cli.path");
			}
			// Force rediscovery to restore normal state
			ClaudeCliDiscovery.forceRediscovery();
		}
	}

}