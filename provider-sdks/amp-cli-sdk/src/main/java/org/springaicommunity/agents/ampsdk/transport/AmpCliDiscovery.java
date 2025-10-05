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

package org.springaicommunity.agents.ampsdk.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.ampsdk.exceptions.AmpSDKException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Discovers the Amp CLI executable on the system.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class AmpCliDiscovery {

	private static final Logger logger = LoggerFactory.getLogger(AmpCliDiscovery.class);

	private static final String ENV_AMP_CLI_PATH = "AMP_CLI_PATH";

	/**
	 * Discovers the Amp CLI executable path.
	 * @return the path to the Amp CLI executable
	 * @throws AmpSDKException if the CLI cannot be found
	 */
	public static String discoverAmpCli() {
		// 1. Check environment variable first (highest priority)
		String envPath = System.getenv(ENV_AMP_CLI_PATH);
		if (envPath != null && !envPath.isEmpty()) {
			Path ampPath = Paths.get(envPath);
			if (Files.exists(ampPath) && Files.isExecutable(ampPath)) {
				logger.info("Found Amp CLI via {} environment variable: {}", ENV_AMP_CLI_PATH, envPath);
				return envPath;
			}
			logger.warn("{} points to non-existent or non-executable file: {}", ENV_AMP_CLI_PATH, envPath);
		}

		// 2. Try 'which amp' on Unix-like systems
		String whichResult = tryWhichCommand();
		if (whichResult != null) {
			logger.info("Found Amp CLI via 'which' command: {}", whichResult);
			return whichResult;
		}

		// 3. Try common installation locations
		String[] commonPaths = { System.getProperty("user.home") + "/.local/bin/amp", "/usr/local/bin/amp",
				"/usr/bin/amp" };

		for (String path : commonPaths) {
			Path ampPath = Paths.get(path);
			if (Files.exists(ampPath) && Files.isExecutable(ampPath)) {
				logger.info("Found Amp CLI at standard location: {}", path);
				return path;
			}
		}

		throw new AmpSDKException(
				"Amp CLI not found. Please install Amp CLI or set " + ENV_AMP_CLI_PATH + " environment variable");
	}

	private static String tryWhichCommand() {
		try {
			ProcessResult result = new ProcessExecutor().command("which", "amp")
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
			logger.debug("'which amp' command failed: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Validates that the Amp CLI is available and functional.
	 * @param ampPath path to the Amp CLI executable
	 * @return true if the CLI is available and functional
	 */
	public static boolean validateAmpCli(String ampPath) {
		try {
			ProcessResult result = new ProcessExecutor().command(ampPath, "--version")
				.readOutput(true)
				.timeout(10, TimeUnit.SECONDS)
				.execute();

			return result.getExitValue() == 0;
		}
		catch (Exception e) {
			logger.warn("Failed to validate Amp CLI at {}: {}", ampPath, e.getMessage());
			return false;
		}
	}

}
