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

package org.springaicommunity.agents.judge.exec.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility for running Maven builds with automatic wrapper detection.
 *
 * <p>
 * Prefers ./mvnw if present in workspace, otherwise falls back to mvn on PATH. Provides
 * structured build execution results for use in judges.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class MavenBuildRunner {

	private static final Logger logger = LoggerFactory.getLogger(MavenBuildRunner.class);

	private static final long DEFAULT_TIMEOUT_MINUTES = 10;

	/**
	 * Run Maven build with default timeout (10 minutes).
	 * @param workspace the Maven project root directory
	 * @param goals Maven goals to execute (e.g., "clean", "compile")
	 * @return build execution result
	 */
	public static BuildResult runBuild(Path workspace, String... goals) {
		return runBuild(workspace, DEFAULT_TIMEOUT_MINUTES, goals);
	}

	/**
	 * Run Maven build with custom timeout.
	 * @param workspace the Maven project root directory
	 * @param timeoutMinutes timeout in minutes
	 * @param goals Maven goals to execute
	 * @return build execution result
	 */
	public static BuildResult runBuild(Path workspace, long timeoutMinutes, String... goals) {
		Instant startTime = Instant.now();

		try {
			// Detect Maven command
			String mavenCmd = detectMavenCommand(workspace);
			logger.info("Running Maven build in: {}", workspace);
			logger.debug("Maven command: {} {}", mavenCmd, String.join(" ", goals));

			// Build command
			List<String> command = new ArrayList<>();
			command.add(mavenCmd);
			for (String goal : goals) {
				command.add(goal);
			}

			// Execute
			ProcessResult result = new ProcessExecutor().command(command)
				.directory(workspace.toFile())
				.timeout(timeoutMinutes, TimeUnit.MINUTES)
				.readOutput(true)
				.exitValueAny() // Don't throw on non-zero exit
				.execute();

			Duration duration = Duration.between(startTime, Instant.now());
			int exitCode = result.getExitValue();
			String output = result.outputUTF8();
			boolean success = exitCode == 0;

			logger.info("Maven build completed. Exit code: {}, Duration: {}s", exitCode, duration.toSeconds());

			return new BuildResult(exitCode, output, duration, success);

		}
		catch (Exception e) {
			Duration duration = Duration.between(startTime, Instant.now());
			logger.error("Failed to execute Maven build", e);
			return new BuildResult(-1, "Error executing Maven: " + e.getMessage(), duration, false);
		}
	}

	/**
	 * Detect Maven wrapper or fallback to mvn.
	 * @param workspace the Maven project root
	 * @return Maven command to use
	 */
	private static String detectMavenCommand(Path workspace) {
		Path mvnw = workspace.resolve("mvnw");
		if (Files.exists(mvnw) && Files.isExecutable(mvnw)) {
			logger.debug("Found Maven wrapper: {}", mvnw);
			return "./mvnw";
		}
		logger.debug("Maven wrapper not found, using 'mvn' from PATH");
		return "mvn";
	}

	/**
	 * Result of Maven build execution.
	 *
	 * @param exitCode process exit code (0 = success)
	 * @param output combined stdout/stderr output
	 * @param duration execution duration
	 * @param success whether build succeeded (exit code == 0)
	 */
	public record BuildResult(int exitCode, String output, Duration duration, boolean success) {
	}

}
