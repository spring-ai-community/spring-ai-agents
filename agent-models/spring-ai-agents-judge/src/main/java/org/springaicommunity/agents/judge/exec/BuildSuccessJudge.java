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

package org.springaicommunity.agents.judge.exec;

import org.springaicommunity.agents.judge.DeterministicJudge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springaicommunity.agents.judge.context.JudgmentContext;

/**
 * Judge that verifies build success by executing build commands.
 *
 * <p>
 * Extends {@link CommandJudge} with build-specific defaults and smart detection of build
 * tool wrappers (./mvnw, ./gradlew) with fallback to PATH-based tools (mvn, gradle).
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Auto-detect Maven wrapper or fallback to mvn
 * Judge judge = BuildSuccessJudge.maven("clean", "compile");
 *
 * // Auto-detect Gradle wrapper or fallback to gradle
 * Judge judge = BuildSuccessJudge.gradle("build");
 *
 * // Custom build command
 * Judge judge = new BuildSuccessJudge("npm run build");
 * }</pre>
 *
 * <p>
 * The judge uses a 10-minute default timeout (longer than CommandJudge's 2 minutes) since
 * builds can take significant time.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class BuildSuccessJudge extends CommandJudge {

	private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(10);

	/**
	 * Create a BuildSuccessJudge with a custom build command.
	 * @param buildCommand the build command to execute
	 */
	public BuildSuccessJudge(String buildCommand) {
		super(buildCommand, 0, BUILD_TIMEOUT);
	}

	/**
	 * Create a Maven build judge with auto-detection of mvnw wrapper.
	 * <p>
	 * Prefers ./mvnw if present in workspace, otherwise falls back to mvn on PATH.
	 * @param goals Maven goals to execute (e.g., "clean", "compile", "test")
	 * @return BuildSuccessJudge configured for Maven
	 */
	public static BuildSuccessJudge maven(String... goals) {
		return new MavenBuildJudge(goals);
	}

	/**
	 * Create a Gradle build judge with auto-detection of gradlew wrapper.
	 * <p>
	 * Prefers ./gradlew if present in workspace, otherwise falls back to gradle on PATH.
	 * @param tasks Gradle tasks to execute (e.g., "build", "test")
	 * @return BuildSuccessJudge configured for Gradle
	 */
	public static BuildSuccessJudge gradle(String... tasks) {
		return new GradleBuildJudge(tasks);
	}

	/**
	 * Maven-specific build judge that detects mvnw wrapper.
	 */
	private static class MavenBuildJudge extends BuildSuccessJudge {

		private final String[] goals;

		MavenBuildJudge(String[] goals) {
			super("mvn " + String.join(" ", goals)); // Temporary, will be resolved in
														// judge()
			this.goals = goals;
		}

		@Override
		public org.springaicommunity.agents.judge.result.Judgment judge(JudgmentContext context) {
			// Detect wrapper in workspace
			String command = detectMavenCommand(context.workspace());
			// Create new judge with detected command
			BuildSuccessJudge actualJudge = new BuildSuccessJudge(command);
			return actualJudge.judge(context);
		}

		private String detectMavenCommand(Path workspace) {
			Path mvnw = workspace.resolve("mvnw");
			if (Files.exists(mvnw) && Files.isExecutable(mvnw)) {
				return "./mvnw " + String.join(" ", goals);
			}
			return "mvn " + String.join(" ", goals);
		}

	}

	/**
	 * Gradle-specific build judge that detects gradlew wrapper.
	 */
	private static class GradleBuildJudge extends BuildSuccessJudge {

		private final String[] tasks;

		GradleBuildJudge(String[] tasks) {
			super("gradle " + String.join(" ", tasks)); // Temporary, will be resolved in
														// judge()
			this.tasks = tasks;
		}

		@Override
		public org.springaicommunity.agents.judge.result.Judgment judge(JudgmentContext context) {
			// Detect wrapper in workspace
			String command = detectGradleCommand(context.workspace());
			// Create new judge with detected command
			BuildSuccessJudge actualJudge = new BuildSuccessJudge(command);
			return actualJudge.judge(context);
		}

		private String detectGradleCommand(Path workspace) {
			Path gradlew = workspace.resolve("gradlew");
			if (Files.exists(gradlew) && Files.isExecutable(gradlew)) {
				return "./gradlew " + String.join(" ", tasks);
			}
			return "gradle " + String.join(" ", tasks);
		}

	}

}
