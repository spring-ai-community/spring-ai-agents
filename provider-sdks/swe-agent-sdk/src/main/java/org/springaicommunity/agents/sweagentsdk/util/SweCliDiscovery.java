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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.InvalidExitValueException;

import java.io.IOException;

/**
 * Centralized discovery class for all SWE Agent CLI detection and interaction. This is
 * the single source of truth for CLI availability checking. All other classes should
 * delegate to this utility to maintain consistency.
 *
 * <p>
 * This utility searches for the mini-swe-agent CLI executable in multiple common
 * locations and provides caching to avoid redundant process executions.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>
 * // Check if SWE Agent CLI is available
 * if (SweCliDiscovery.isSweCliAvailable()) {
 *     String cliPath = SweCliDiscovery.getDiscoveredPath();
 *     // Use the CLI...
 * }
 * </pre>
 */
public class SweCliDiscovery {

	private static final Logger logger = LoggerFactory.getLogger(SweCliDiscovery.class);

	// Cache for CLI availability results to avoid redundant checks
	private static volatile CliAvailabilityResult cachedResult = null;

	private static volatile String cachedCommand = null;

	private static final Object cacheLock = new Object();

	/**
	 * Finds the SWE Agent CLI command, trying multiple common locations. This is
	 * especially useful for integration tests running in IDEs where PATH might not
	 * include CLI tools.
	 * @return the path to the SWE Agent CLI command
	 */
	public static String findSweCommand() {
		// First check if a specific path was provided via system property
		String systemPath = System.getProperty("swe.cli.path");
		if (systemPath != null) {
			if (isCommandAvailable(systemPath)) {
				logger.trace("Found SWE Agent CLI at system property path: {}", systemPath);
				return systemPath;
			}
			else {
				logger.warn("SWE Agent CLI not found at system property path: {}", systemPath);
			}
		}

		// Potential SWE Agent CLI locations
		String[] possibleCommands = { "mini", // Standard PATH lookup
				"/home/mark/.local/bin/mini", // Common installation location
				System.getProperty("user.home") + "/.local/bin/mini", // Dynamic user home
				"/usr/local/bin/mini", // Common global install location
				"/opt/homebrew/bin/mini", // Homebrew on macOS
				"/usr/bin/mini", // System bin
				System.getProperty("user.home") + "/.nvm/versions/node/v22.15.0/bin/mini", // NVM
																							// specific
				"/home/mark/.nvm/versions/node/v22.15.0/bin/mini" // Development
																	// environment
		};

		for (String command : possibleCommands) {
			if (isCommandAvailable(command)) {
				logger.trace("Found SWE Agent CLI at: {}", command);
				return command;
			}
		}

		// Fallback to "mini" if nothing else works
		logger.warn("SWE Agent CLI not found at any known location, falling back to PATH lookup");
		return "mini";
	}

	/**
	 * Performs comprehensive CLI availability check for a specific command. This is the
	 * authoritative method for testing CLI availability. Results are cached to avoid
	 * redundant process executions.
	 * @param command the command path to test
	 * @return detailed availability information
	 */
	public static CliAvailabilityResult checkCommandAvailability(String command) {
		// Check cache first
		if (cachedResult != null && command.equals(cachedCommand)) {
			logger.trace("Using cached CLI availability result for: {}", command);
			return cachedResult;
		}

		synchronized (cacheLock) {
			// Double-check pattern
			if (cachedResult != null && command.equals(cachedCommand)) {
				return cachedResult;
			}

			CliAvailabilityResult result = performCliCheck(command);

			// Cache the result
			cachedCommand = command;
			cachedResult = result;

			return result;
		}
	}

	/**
	 * Actually performs the CLI availability check without caching.
	 */
	private static CliAvailabilityResult performCliCheck(String command) {
		// Try --version first (standard approach)
		CliAvailabilityResult versionResult = tryVersionCheck(command);
		if (versionResult.isAvailable()) {
			return versionResult;
		}

		// mini-swe-agent doesn't support --version, try --help instead
		CliAvailabilityResult helpResult = tryHelpCheck(command);
		if (helpResult.isAvailable()) {
			return helpResult;
		}

		// Last resort: try running without args (some CLIs show version on error)
		CliAvailabilityResult noArgsResult = tryNoArgsCheck(command);
		if (noArgsResult.isAvailable()) {
			return noArgsResult;
		}

		// If all approaches failed, return the most informative error
		logger.trace("All CLI availability checks failed for {}", command);
		return versionResult; // Return the first failure for consistency
	}

	/**
	 * Attempts to check CLI availability using --version flag.
	 */
	private static CliAvailabilityResult tryVersionCheck(String command) {
		try {
			ProcessExecutor executor = new ProcessExecutor().command(command, "--version")
				.timeout(10, java.util.concurrent.TimeUnit.SECONDS)
				.readOutput(true)
				.exitValueNormal();

			ProcessResult result = executor.execute();
			String version = result.outputUTF8().trim();
			logger.trace("CLI availability check successful for {}: {}", command, version);
			return CliAvailabilityResult.available(version);

		}
		catch (InvalidExitValueException e) {
			String output = e.getResult().outputUTF8();
			String stderr = getStderr(e.getResult());
			String reason = String.format("CLI exited with code %d. Output: %s, Error: %s", e.getExitValue(), output,
					stderr);
			logger.trace("CLI --version check failed for {}: {}", command, reason);
			return CliAvailabilityResult.unavailable(reason, e);

		}
		catch (java.util.concurrent.TimeoutException e) {
			String reason = "CLI version check timed out after 10 seconds";
			logger.trace("CLI --version check timed out for {}: {}", command, reason);
			return CliAvailabilityResult.unavailable(reason, e);

		}
		catch (IOException e) {
			String reason = "CLI not found or not executable: " + e.getMessage();
			logger.trace("CLI not found: {} ({})", command, reason);
			return CliAvailabilityResult.unavailable(reason, e);

		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			String reason = "CLI availability check was interrupted";
			logger.debug("CLI --version check interrupted for {}: {}", command, reason);
			return CliAvailabilityResult.unavailable(reason, e);

		}
		catch (Exception e) {
			String reason = "Unexpected error during CLI check: " + e.getMessage();
			logger.trace("Unexpected error checking CLI availability for {}: {}", command, reason);
			return CliAvailabilityResult.unavailable(reason, e);
		}
	}

	/**
	 * Attempts to check CLI availability using --help flag (for CLIs that show version in
	 * help).
	 */
	private static CliAvailabilityResult tryHelpCheck(String command) {
		try {
			ProcessExecutor executor = new ProcessExecutor().command(command, "--help")
				.timeout(10, java.util.concurrent.TimeUnit.SECONDS)
				.readOutput(true);

			ProcessResult result = executor.execute();
			String output = result.outputUTF8();

			// Look for version patterns in help output
			String version = extractVersionFromOutput(output);
			if (version != null) {
				logger.trace("CLI availability check successful via --help for {}: {}", command, version);
				return CliAvailabilityResult.available(version);
			}

			// Even if no version found, if --help worked, CLI is available
			if (result.getExitValue() == 0) {
				logger.trace("CLI availability confirmed via --help for {} (no version found)", command);
				return CliAvailabilityResult.available("unknown version");
			}

		}
		catch (Exception e) {
			logger.trace("CLI --help check failed for {}: {}", command, e.getMessage());
		}

		return CliAvailabilityResult.unavailable("CLI --help check failed", null);
	}

	/**
	 * Attempts to check CLI availability by running without arguments (for CLIs that show
	 * version on error).
	 */
	private static CliAvailabilityResult tryNoArgsCheck(String command) {
		try {
			ProcessExecutor executor = new ProcessExecutor().command(command)
				.timeout(10, java.util.concurrent.TimeUnit.SECONDS)
				.readOutput(true);

			ProcessResult result = executor.execute();
			String output = result.outputUTF8();

			// Look for version patterns in output (even from stderr/error cases)
			String version = extractVersionFromOutput(output);
			if (version != null) {
				logger.trace("CLI availability check successful via no-args for {}: {}", command, version);
				return CliAvailabilityResult.available(version);
			}

			// For mini-swe-agent, even an error might indicate the CLI is available
			if (output.contains("mini-swe-agent") || output.contains("Usage:")) {
				logger.trace("CLI availability confirmed via no-args for {} (usage detected)", command);
				return CliAvailabilityResult.available("detected from usage");
			}

		}
		catch (Exception e) {
			logger.trace("CLI no-args check failed for {}: {}", command, e.getMessage());
		}

		return CliAvailabilityResult.unavailable("CLI no-args check failed", null);
	}

	/**
	 * Extracts version information from CLI output using common patterns.
	 */
	private static String extractVersionFromOutput(String output) {
		if (output == null || output.trim().isEmpty()) {
			return null;
		}

		// Common version patterns
		String[] patterns = { "mini-swe-agent version ([0-9]+\\.[0-9]+\\.[0-9]+)", "version ([0-9]+\\.[0-9]+\\.[0-9]+)",
				"v([0-9]+\\.[0-9]+\\.[0-9]+)", "([0-9]+\\.[0-9]+\\.[0-9]+)" };

		for (String pattern : patterns) {
			java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern,
					java.util.regex.Pattern.CASE_INSENSITIVE);
			java.util.regex.Matcher matcher = regex.matcher(output);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return null;
	}

	/**
	 * Clears the CLI availability cache. Useful for testing or when CLI state may have
	 * changed.
	 */
	public static void clearCache() {
		synchronized (cacheLock) {
			cachedResult = null;
			cachedCommand = null;
			logger.debug("CLI availability cache cleared");
		}
	}

	/**
	 * Simple availability check for a specific command.
	 * @param command the command path to test
	 * @return true if the command is available and responds to --version
	 */
	public static boolean isCommandAvailable(String command) {
		return checkCommandAvailability(command).isAvailable();
	}

	/**
	 * Performs comprehensive SWE Agent CLI availability check. This is the authoritative
	 * method for all CLI availability testing.
	 * @return detailed availability information
	 */
	public static CliAvailabilityResult checkSweCliAvailability() {
		String command = findSweCommand();
		return checkCommandAvailability(command);
	}

	/**
	 * Checks if SWE Agent CLI is available anywhere on the system.
	 * @return true if SWE Agent CLI can be found and executed
	 */
	public static boolean isSweCliAvailable() {
		return checkSweCliAvailability().isAvailable();
	}

	/**
	 * Gets the version of the available SWE Agent CLI.
	 * @return the version string, or null if CLI is not available
	 */
	public static String getSweCliVersion() {
		CliAvailabilityResult result = checkSweCliAvailability();
		return result.isAvailable() ? result.getVersion().orElse(null) : null;
	}

	/**
	 * Gets the discovered path to the SWE Agent CLI executable. This method matches the
	 * pattern used by ClaudeCliDiscovery.
	 * @return the discovered path to the CLI, or null if not available
	 */
	public static String getDiscoveredPath() {
		CliAvailabilityResult result = checkSweCliAvailability();
		return result.isAvailable() ? findSweCommand() : null;
	}

	/**
	 * Extracts stderr from ProcessResult, handling potential encoding issues.
	 */
	private static String getStderr(ProcessResult result) {
		try {
			// zt-exec captures both stdout and stderr in the output by default
			// For failed processes, we can provide more context by examining the output
			String output = result.outputUTF8();
			if (output != null && (output.contains("Error:") || output.contains("error:"))) {
				return output; // Return the full output which may contain error info
			}
			return "No stderr captured"; // Indicates process failed but no obvious error
											// message
		}
		catch (Exception e) {
			logger.warn("Failed to extract stderr: {}", e.getMessage());
			return "stderr extraction failed: " + e.getMessage();
		}
	}

}