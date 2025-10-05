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

package org.springaicommunity.agents.codexsdk.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.codexsdk.exceptions.CodexSDKException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Discovers the Codex CLI executable on the system.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class CodexCliDiscovery {

	private static final Logger logger = LoggerFactory.getLogger(CodexCliDiscovery.class);

	private static final String ENV_CODEX_CLI_PATH = "CODEX_CLI_PATH";

	/**
	 * Discovers the Codex CLI executable path.
	 * @return the path to the Codex CLI executable
	 * @throws CodexSDKException if the CLI cannot be found
	 */
	public static String discoverCodexCli() {
		// 1. Check environment variable first (highest priority)
		String envPath = System.getenv(ENV_CODEX_CLI_PATH);
		if (envPath != null && !envPath.isEmpty()) {
			Path codexPath = Paths.get(envPath);
			if (Files.exists(codexPath) && Files.isExecutable(codexPath)) {
				logger.info("Found Codex CLI via {} environment variable: {}", ENV_CODEX_CLI_PATH, envPath);
				return envPath;
			}
			logger.warn("{} points to non-existent or non-executable file: {}", ENV_CODEX_CLI_PATH, envPath);
		}

		// 2. Try 'which codex' on Unix-like systems
		String whichResult = tryWhichCommand();
		if (whichResult != null) {
			logger.info("Found Codex CLI via 'which' command: {}", whichResult);
			return whichResult;
		}

		// 3. Try common npm global installation locations
		String homeDir = System.getProperty("user.home");
		String[] commonPaths = { homeDir + "/.nvm/versions/node/v22.15.0/bin/codex", // NVM
																						// path
																						// (current
																						// env)
				homeDir + "/.local/bin/codex", // Local bin
				"/usr/local/bin/codex", // Homebrew default
				"/usr/bin/codex" // System bin
		};

		for (String path : commonPaths) {
			Path codexPath = Paths.get(path);
			if (Files.exists(codexPath) && Files.isExecutable(codexPath)) {
				logger.info("Found Codex CLI at standard location: {}", path);
				return path;
			}
		}

		throw new CodexSDKException("Codex CLI not found. Please install via 'npm install -g @openai/codex' or "
				+ "'brew install codex', or set " + ENV_CODEX_CLI_PATH + " environment variable");
	}

	private static String tryWhichCommand() {
		try {
			ProcessResult result = new ProcessExecutor().command("which", "codex")
				.readOutput(true)
				.timeout(5, TimeUnit.SECONDS)
				.execute();

			if (result.getExitValue() == 0) {
				String path = result.outputUTF8().trim();
				if (!path.isEmpty()) {
					File file = new File(path);
					if (file.exists() && file.canExecute()) {
						return path;
					}
				}
			}
		}
		catch (Exception e) {
			logger.debug("'which codex' command failed: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Validates that the Codex CLI is available and functional.
	 * @param codexPath path to the Codex CLI executable
	 * @return true if the CLI is available and functional
	 */
	public static boolean validateCodexCli(String codexPath) {
		try {
			ProcessResult result = new ProcessExecutor().command(codexPath, "--version")
				.readOutput(true)
				.timeout(10, TimeUnit.SECONDS)
				.execute();

			if (result.getExitValue() == 0) {
				logger.debug("Codex CLI version: {}", result.outputUTF8().trim());
				return true;
			}
		}
		catch (Exception e) {
			logger.warn("Failed to validate Codex CLI at {}: {}", codexPath, e.getMessage());
		}
		return false;
	}

}
