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

package org.springaicommunity.agents.amazonqsdk.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.amazonqsdk.exceptions.AmazonQSDKException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Discovers the Amazon Q CLI executable on the system.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class AmazonQCliDiscovery {

	private static final Logger logger = LoggerFactory.getLogger(AmazonQCliDiscovery.class);

	private static final String ENV_Q_CLI_PATH = "Q_CLI_PATH";

	/**
	 * Discovers the Amazon Q CLI executable path.
	 * @return the path to the Q CLI executable
	 * @throws AmazonQSDKException if Q CLI cannot be found
	 */
	public static String discoverQCli() {
		// 1. Check Q_CLI_PATH environment variable
		String envPath = System.getenv(ENV_Q_CLI_PATH);
		if (envPath != null && !envPath.isEmpty()) {
			Path qPath = Paths.get(envPath);
			if (Files.exists(qPath) && Files.isExecutable(qPath)) {
				logger.info("Found Amazon Q CLI via Q_CLI_PATH: {}", envPath);
				return envPath;
			}
			logger.warn("Q_CLI_PATH set but not executable: {}", envPath);
		}

		// 2. Try 'which q' command
		String whichResult = tryWhichCommand();
		if (whichResult != null) {
			logger.info("Found Amazon Q CLI via 'which q': {}", whichResult);
			return whichResult;
		}

		// 3. Check common installation paths
		String homeDir = System.getProperty("user.home");
		String[] commonPaths = { homeDir + "/.local/bin/q", "/usr/local/bin/q", "/usr/bin/q" };

		for (String path : commonPaths) {
			Path qPath = Paths.get(path);
			if (Files.exists(qPath) && Files.isExecutable(qPath)) {
				logger.info("Found Amazon Q CLI at: {}", path);
				return path;
			}
		}

		throw new AmazonQSDKException(
				"Amazon Q CLI not found. Install from https://aws.amazon.com/q/developer/ or set Q_CLI_PATH environment variable.");
	}

	private static String tryWhichCommand() {
		try {
			ProcessResult result = new ProcessExecutor().command("which", "q")
				.timeout(5, TimeUnit.SECONDS)
				.readOutput(true)
				.execute();

			if (result.getExitValue() == 0) {
				String output = result.outputUTF8().trim();
				if (!output.isEmpty()) {
					return output;
				}
			}
		}
		catch (Exception e) {
			logger.debug("'which q' command failed", e);
		}
		return null;
	}

	/**
	 * Checks if Amazon Q CLI is available on this system.
	 * @return true if Q CLI is available
	 */
	public static boolean isAvailable() {
		try {
			String qPath = discoverQCli();
			return qPath != null;
		}
		catch (AmazonQSDKException e) {
			return false;
		}
	}

}
