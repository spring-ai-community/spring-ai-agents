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

package org.springaicommunity.agents.judge.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CommandJudgeTest {

	@TempDir
	Path tempDir;

	@Test
	void successfulCommandPassesJudgment() throws Exception {
		// Create a test file
		Path testFile = tempDir.resolve("test.txt");
		Files.writeString(testFile, "test content");

		// Command that should succeed (list files)
		CommandJudge judge = new CommandJudge("ls test.txt");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.reasoning()).contains("succeeded").contains("exit code 0");
		assertThat(judgment.checks()).hasSize(1);
		assertThat(judgment.checks().get(0).passed()).isTrue();
		assertThat(judgment.checks().get(0).name()).isEqualTo("command_execution");

		// Verify metadata
		assertThat(judgment.metadata()).containsEntry("command", "ls test.txt").containsEntry("exitCode", 0);
	}

	@Test
	void failingCommandFailsJudgment() {
		// Command that should fail (non-existent command)
		CommandJudge judge = new CommandJudge("nonexistentcommand123");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isFalse();
		assertThat(judgment.checks()).hasSize(1);
		assertThat(judgment.checks().get(0).passed()).isFalse();
	}

	@Test
	void customExitCodeJudgment() {
		// Command that exits with code 1 (grep with no match)
		CommandJudge judge = new CommandJudge("grep 'nonexistent' /dev/null", 1, Duration.ofSeconds(5));
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue(); // Expects exit code 1
		assertThat(judgment.metadata()).containsEntry("expectedExitCode", 1).containsEntry("exitCode", 1);
	}

	@Test
	void commandOutputCapturedInMetadata() throws Exception {
		// Create test file with content
		Path testFile = tempDir.resolve("output.txt");
		Files.writeString(testFile, "Hello Judge");

		CommandJudge judge = new CommandJudge("cat output.txt");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.metadata()).containsKey("output");
		assertThat((String) judgment.metadata().get("output")).contains("Hello Judge");
	}

	@Test
	void metadataIncludesCommandDetails() {
		CommandJudge judge = new CommandJudge("echo 'test'", 0, Duration.ofSeconds(10));
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.metadata()).containsEntry("command", "echo 'test'")
			.containsEntry("expectedExitCode", 0)
			.containsKey("exitCode")
			.containsKey("output")
			.containsKey("duration");
	}

	private JudgmentContext createContext() {
		return JudgmentContext.builder()
			.goal("Test goal")
			.workspace(tempDir)
			.executionTime(Duration.ofSeconds(1))
			.startedAt(Instant.now())
			.status(AgentExecutionStatus.SUCCESS)
			.build();
	}

}
