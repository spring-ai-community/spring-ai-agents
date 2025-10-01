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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.util.concurrent.TimeUnit;

/**
 * Utility for discovering Claude CLI executable location across different environments.
 *
 * <p>
 * This utility attempts to find the Claude CLI executable in common installation
 * locations and provides a fallback for development environments.
 * </p>
 */
public class ClaudeCliDiscovery {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeCliDiscovery.class);

	private static final String FALLBACK_PATH = "/home/mark/.nvm/versions/node/v22.15.0/bin/claude";

	private static String discoveredPath;

	private static boolean discoveryAttempted = false;

	/**
	 * Discovers the Claude CLI executable path.
	 * @return the path to Claude CLI executable
	 * @throws ClaudeCliNotFoundException if Claude CLI cannot be found
	 */
	public static synchronized String discoverClaudePath() throws ClaudeCliNotFoundException {
		if (discoveryAttempted) {
			if (discoveredPath != null) {
				return discoveredPath;
			}
			else {
				throw new ClaudeCliNotFoundException("Claude CLI was not found during discovery");
			}
		}

		discoveryAttempted = true;

		// Check system property first
		String systemPropertyPath = System.getProperty("claude.cli.path");
		if (systemPropertyPath != null) {
			String resolvedPath = testAndResolveClaudeExecutable(systemPropertyPath);
			if (resolvedPath != null) {
				discoveredPath = resolvedPath;
				logger.info("Claude CLI found at system property: {}", resolvedPath);
				return discoveredPath;
			}
			else {
				// If system property is set but doesn't work, fail immediately
				throw new ClaudeCliNotFoundException(
						"Claude CLI specified in system property 'claude.cli.path' is not available: "
								+ systemPropertyPath);
			}
		}

		// Attempt discovery in order of preference
		String[] candidates = { "claude", // In PATH
				"claude-code", // Alternative name in PATH
				System.getProperty("user.home") + "/.local/bin/claude", // Local
																		// installation
				"/usr/local/bin/claude", // System-wide installation
				"/opt/claude/bin/claude", // Alternative system location
				System.getProperty("user.home") + "/.nvm/versions/node/v22.15.0/bin/claude", // NVM
																								// installation
				System.getProperty("user.home") + "/.nvm/versions/node/latest/bin/claude", // Latest
																							// NVM
				"/usr/bin/claude", // Standard system path
				FALLBACK_PATH // Development fallback
		};

		for (String candidate : candidates) {
			String resolvedPath = testAndResolveClaudeExecutable(candidate);
			if (resolvedPath != null) {
				discoveredPath = resolvedPath;
				logger.info("Claude CLI found at: {}", resolvedPath);
				return discoveredPath;
			}
		}

		// If discovery fails, provide detailed error message
		StringBuilder errorMessage = new StringBuilder();
		errorMessage.append("Claude CLI executable not found. Searched locations:\n");
		for (String candidate : candidates) {
			errorMessage.append("  - ").append(candidate).append("\n");
		}
		errorMessage.append("\nPlease ensure Claude CLI is installed and accessible.\n");
		errorMessage.append("Visit: https://github.com/anthropics/claude-code for installation instructions");

		throw new ClaudeCliNotFoundException(errorMessage.toString());
	}

	/**
	 * Tests if a Claude CLI executable exists and works at the given path. When the path
	 * is a command name (like "claude"), this resolves it to the full path.
	 * @param path command name or full path to test
	 * @return the resolved full path if the executable works, null if not found
	 */
	private static String testAndResolveClaudeExecutable(String path) {
		try {
			ProcessResult result = new ProcessExecutor().command(path, "--version")
				.timeout(5, TimeUnit.SECONDS)
				.readOutput(true)
				.execute();

			if (result.getExitValue() == 0) {
				String version = result.outputUTF8().trim();
				logger.debug("Found Claude CLI at {} with version: {}", path, version);

				// If this is just a command name (no path separators), resolve to full
				// path
				if (!path.contains("/") && !path.contains("\\")) {
					String resolvedPath = resolveCommandPath(path);
					if (resolvedPath != null) {
						logger.debug("Resolved command '{}' to full path: {}", path, resolvedPath);
						return resolvedPath;
					}
				}

				// Return the original path (already a full path)
				return path;
			}
		}
		catch (Exception e) {
			logger.debug("Claude CLI not found at: {} ({})", path, e.getMessage());
		}
		return null;
	}

	/**
	 * Resolves a command name to its full path using platform-appropriate commands. Uses
	 * 'which' on Unix/Linux/macOS and 'where' on Windows.
	 */
	private static String resolveCommandPath(String commandName) {
		try {
			String osName = System.getProperty("os.name").toLowerCase();
			String[] command;

			if (osName.contains("win")) {
				// Windows uses 'where'
				command = new String[] { "where", commandName };
			}
			else {
				// Unix/Linux/macOS use 'which'
				command = new String[] { "which", commandName };
			}

			ProcessResult result = new ProcessExecutor().command(command)
				.timeout(3, TimeUnit.SECONDS)
				.readOutput(true)
				.execute();

			if (result.getExitValue() == 0) {
				String output = result.outputUTF8().trim();
				// Windows 'where' can return multiple paths, take the first one
				if (osName.contains("win") && output.contains("\n")) {
					output = output.split("\n")[0].trim();
				}
				return output;
			}
		}
		catch (Exception e) {
			logger.debug("Failed to resolve command path for '{}': {}", commandName, e.getMessage());
		}
		return null;
	}

	/**
	 * Gets the discovered Claude CLI path without performing discovery. Used for cases
	 * where discovery has already been performed.
	 */
	public static String getDiscoveredPath() {
		return discoveredPath;
	}

	/**
	 * Checks if Claude CLI is available without throwing exceptions.
	 * @return true if Claude CLI is available, false otherwise
	 */
	public static boolean isClaudeCliAvailable() {
		try {
			discoverClaudePath();
			return true;
		}
		catch (ClaudeCliNotFoundException e) {
			return false;
		}
	}

	/**
	 * Forces re-discovery of Claude CLI path. Useful for testing or when installation
	 * state may have changed.
	 */
	public static synchronized void forceRediscovery() {
		discoveryAttempted = false;
		discoveredPath = null;
	}

	/**
	 * Exception thrown when Claude CLI cannot be discovered.
	 */
	public static class ClaudeCliNotFoundException extends Exception {

		public ClaudeCliNotFoundException(String message) {
			super(message);
		}

	}

}