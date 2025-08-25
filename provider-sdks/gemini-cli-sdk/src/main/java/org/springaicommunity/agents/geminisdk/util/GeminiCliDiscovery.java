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

import org.springaicommunity.agents.geminisdk.transport.CliAvailabilityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.InvalidExitValueException;

import java.io.IOException;

/**
 * Centralized discovery class for all Gemini CLI detection and interaction. This is the
 * single source of truth for CLI availability checking. All other classes should delegate
 * to this utility to maintain consistency.
 */
public class GeminiCliDiscovery {

	private static final Logger logger = LoggerFactory.getLogger(GeminiCliDiscovery.class);

	// Cache for CLI availability results to avoid redundant checks
	private static volatile CliAvailabilityResult cachedResult = null;

	private static volatile String cachedCommand = null;

	private static final Object cacheLock = new Object();

	/**
	 * Finds the Gemini CLI command, trying multiple common locations. This is especially
	 * useful for integration tests running in IDEs where PATH might not include CLI
	 * tools.
	 * @return the path to the Gemini CLI command
	 */
	public static String findGeminiCommand() {
		// First check if a specific path was provided via system property
		String systemPath = System.getProperty("gemini.cli.path");
		if (systemPath != null) {
			if (isCommandAvailable(systemPath)) {
				logger.trace("Found Gemini CLI at system property path: {}", systemPath);
				return systemPath;
			}
			else {
				logger.warn("Gemini CLI not found at system property path: {}", systemPath);
			}
		}

		// Potential Gemini CLI locations
		String[] possibleCommands = { "gemini", // Standard PATH lookup
				"/home/mark/.nvm/versions/node/v22.15.0/bin/gemini", // Specific nvm
																		// location for
																		// development
				System.getProperty("user.home") + "/.nvm/versions/node/v22.15.0/bin/gemini", // Dynamic
																								// user
																								// home
				"/usr/local/bin/gemini", // Common global install location
				"/opt/homebrew/bin/gemini", // Homebrew on macOS
				System.getProperty("user.home") + "/.local/bin/gemini", // User local bin
				"/usr/bin/gemini" // System bin
		};

		for (String command : possibleCommands) {
			if (isCommandAvailable(command)) {
				logger.trace("Found Gemini CLI at: {}", command);
				return command;
			}
		}

		// Fallback to "gemini" if nothing else works
		logger.warn("Gemini CLI not found at any known location, falling back to PATH lookup");
		return "gemini";
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
		try {
			ProcessExecutor executor;

			// If using nvm-installed CLI, execute it with the correct Node.js to avoid
			// shebang issues
			if (command.contains("/.nvm/")) {
				String nvmNodePath = extractNvmNodePath(command);
				if (nvmNodePath != null) {
					String nodePath = nvmNodePath + "/node";
					executor = new ProcessExecutor().command(nodePath, command, "--version")
						.timeout(10, java.util.concurrent.TimeUnit.SECONDS)
						.readOutput(true)
						.exitValueNormal();
					logger.trace("Using direct Node.js execution: {} {}", nodePath, command);
				}
				else {
					executor = new ProcessExecutor().command(command, "--version")
						.timeout(10, java.util.concurrent.TimeUnit.SECONDS)
						.readOutput(true)
						.exitValueNormal();
				}
			}
			else {
				executor = new ProcessExecutor().command(command, "--version")
					.timeout(10, java.util.concurrent.TimeUnit.SECONDS)
					.readOutput(true)
					.exitValueNormal();
			}

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
			logger.trace("CLI availability check failed for {}: {}", command, reason);
			return CliAvailabilityResult.unavailable(reason, e);

		}
		catch (java.util.concurrent.TimeoutException e) {
			String reason = "CLI version check timed out after 10 seconds";
			logger.trace("CLI availability check timed out for {}: {}", command, reason);
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
			logger.debug("CLI availability check interrupted for {}: {}", command, reason);
			return CliAvailabilityResult.unavailable(reason, e);

		}
		catch (Exception e) {
			String reason = "Unexpected error during CLI check: " + e.getMessage();
			logger.trace("Unexpected error checking CLI availability for {}: {}", command, reason);
			return CliAvailabilityResult.unavailable(reason, e);
		}
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
	 * Performs comprehensive Gemini CLI availability check. This is the authoritative
	 * method for all CLI availability testing.
	 * @return detailed availability information
	 */
	public static CliAvailabilityResult checkGeminiCliAvailability() {
		String command = findGeminiCommand();
		return checkCommandAvailability(command);
	}

	/**
	 * Checks if Gemini CLI is available anywhere on the system.
	 * @return true if Gemini CLI can be found and executed
	 */
	public static boolean isGeminiCliAvailable() {
		return checkGeminiCliAvailability().isAvailable();
	}

	/**
	 * Gets the version of the available Gemini CLI.
	 * @return the version string, or null if CLI is not available
	 */
	public static String getGeminiCliVersion() {
		CliAvailabilityResult result = checkGeminiCliAvailability();
		return result.isAvailable() ? result.getVersion().orElse(null) : null;
	}

	/**
	 * Gets the discovered path to the Gemini CLI executable. This method matches the
	 * pattern used by ClaudeCliDiscovery.
	 * @return the discovered path to the CLI, or null if not available
	 */
	public static String getDiscoveredPath() {
		CliAvailabilityResult result = checkGeminiCliAvailability();
		return result.isAvailable() ? findGeminiCommand() : null;
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

	/**
	 * Extracts the Node.js bin directory from an nvm gemini path.
	 * @param geminiPath path like /home/user/.nvm/versions/node/v22.15.0/bin/gemini
	 * @return the bin directory path, or null if not an nvm path
	 */
	public static String extractNvmNodePath(String geminiPath) {
		if (geminiPath.contains("/.nvm/") && geminiPath.contains("/bin/gemini")) {
			return geminiPath.substring(0, geminiPath.lastIndexOf("/gemini"));
		}
		return null;
	}

	/**
	 * Gets the command array for executing Gemini CLI with proper Node.js version.
	 * @param geminiCommand the path to gemini command
	 * @param args additional arguments for the CLI
	 * @return command array ready for ProcessExecutor
	 */
	public static String[] getGeminiCommand(String geminiCommand, String... args) {
		if (geminiCommand.contains("/.nvm/")) {
			String nvmNodePath = extractNvmNodePath(geminiCommand);
			if (nvmNodePath != null) {
				String nodePath = nvmNodePath + "/node";
				String[] command = new String[args.length + 2];
				command[0] = nodePath;
				command[1] = geminiCommand;
				System.arraycopy(args, 0, command, 2, args.length);
				return command;
			}
		}

		// Fallback to direct execution
		String[] command = new String[args.length + 1];
		command[0] = geminiCommand;
		System.arraycopy(args, 0, command, 1, args.length);
		return command;
	}

}