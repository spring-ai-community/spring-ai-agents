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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BuildSuccessJudgeTest {

	@TempDir
	Path tempDir;

	@Test
	void customBuildCommandExecutes() {
		BuildSuccessJudge judge = new BuildSuccessJudge("echo 'Building...'");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.reasoning()).contains("succeeded");
	}

	@Test
	void mavenDetectsWrapperWhenPresent() throws IOException {
		// Create mvnw wrapper
		Path mvnw = tempDir.resolve("mvnw");
		Files.writeString(mvnw, "#!/bin/bash\necho 'Maven wrapper'\nexit 0");
		makeExecutable(mvnw);

		BuildSuccessJudge judge = BuildSuccessJudge.maven("--version");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		// Verify it used wrapper (check output contains wrapper script output)
		String output = (String) judgment.metadata().get("output");
		assertThat(output).contains("Maven wrapper");
	}

	@Test
	void mavenFallsBackToMvnWhenWrapperMissing() {
		// No mvnw in tempDir
		BuildSuccessJudge judge = BuildSuccessJudge.maven("--version");
		Judgment judgment = judge.judge(createContext());

		// This will use system 'mvn' which may or may not exist
		// We just verify the judge executes (pass/fail depends on system)
		assertThat(judgment).isNotNull();
		assertThat(judgment.metadata()).containsKey("command");
	}

	@Test
	void gradleDetectsWrapperWhenPresent() throws IOException {
		// Create gradlew wrapper
		Path gradlew = tempDir.resolve("gradlew");
		Files.writeString(gradlew, "#!/bin/bash\necho 'Gradle wrapper'\nexit 0");
		makeExecutable(gradlew);

		BuildSuccessJudge judge = BuildSuccessJudge.gradle("--version");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		String output = (String) judgment.metadata().get("output");
		assertThat(output).contains("Gradle wrapper");
	}

	@Test
	void gradleFallsBackToGradleWhenWrapperMissing() {
		// No gradlew in tempDir
		BuildSuccessJudge judge = BuildSuccessJudge.gradle("--version");
		Judgment judgment = judge.judge(createContext());

		// This will use system 'gradle' which may or may not exist
		assertThat(judgment).isNotNull();
		assertThat(judgment.metadata()).containsKey("command");
	}

	@Test
	void mavenSupportsMultipleGoals() throws IOException {
		Path mvnw = tempDir.resolve("mvnw");
		Files.writeString(mvnw, "#!/bin/bash\necho \"Goals: $@\"\nexit 0");
		makeExecutable(mvnw);

		BuildSuccessJudge judge = BuildSuccessJudge.maven("clean", "compile", "test");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		String output = (String) judgment.metadata().get("output");
		assertThat(output).contains("clean compile test");
	}

	@Test
	void gradleSupportsMultipleTasks() throws IOException {
		Path gradlew = tempDir.resolve("gradlew");
		Files.writeString(gradlew, "#!/bin/bash\necho \"Tasks: $@\"\nexit 0");
		makeExecutable(gradlew);

		BuildSuccessJudge judge = BuildSuccessJudge.gradle("clean", "build", "test");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		String output = (String) judgment.metadata().get("output");
		assertThat(output).contains("clean build test");
	}

	@Test
	void buildFailureDetected() throws IOException {
		// Create failing build script
		Path mvnw = tempDir.resolve("mvnw");
		Files.writeString(mvnw, "#!/bin/bash\necho 'BUILD FAILURE'\nexit 1");
		makeExecutable(mvnw);

		BuildSuccessJudge judge = BuildSuccessJudge.maven("compile");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isFalse();
		assertThat(judgment.reasoning()).contains("failed");
	}

	private JudgmentContext createContext() {
		return JudgmentContext.builder()
			.goal("Build project")
			.workspace(tempDir)
			.executionTime(Duration.ofSeconds(1))
			.startedAt(Instant.now())
			.status(AgentExecutionStatus.SUCCESS)
			.build();
	}

	private void makeExecutable(Path file) throws IOException {
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			// Windows - file is executable by default
			return;
		}
		// Unix-like - set executable permission
		Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
		perms.add(PosixFilePermission.OWNER_EXECUTE);
		perms.add(PosixFilePermission.GROUP_EXECUTE);
		perms.add(PosixFilePermission.OTHERS_EXECUTE);
		Files.setPosixFilePermissions(file, perms);
	}

}
