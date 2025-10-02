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
import org.springaicommunity.agents.judge.impl.FileContentJudge.MatchMode;
import org.springaicommunity.agents.judge.result.Judgment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FileContentJudgeTest {

	@TempDir
	Path tempDir;

	@Test
	void exactMatchPassesWhenContentMatches() throws IOException {
		Path testFile = tempDir.resolve("test.txt");
		Files.writeString(testFile, "Hello World");

		FileContentJudge judge = new FileContentJudge("test.txt", "Hello World", MatchMode.EXACT);
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.reasoning()).contains("exact").contains("matches");
		assertThat(judgment.checks()).hasSize(3);
		assertThat(judgment.checks()).allMatch(check -> check.passed());
	}

	@Test
	void exactMatchFailsWhenContentDiffers() throws IOException {
		Path testFile = tempDir.resolve("test.txt");
		Files.writeString(testFile, "Hello World");

		FileContentJudge judge = new FileContentJudge("test.txt", "Goodbye World", MatchMode.EXACT);
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isFalse();
		assertThat(judgment.checks()).hasSize(3);
		assertThat(judgment.checks().get(2).passed()).isFalse();
		assertThat(judgment.checks().get(2).name()).isEqualTo("content_match");
	}

	@Test
	void containsMatchPassesWhenContentContainsString() throws IOException {
		Path testFile = tempDir.resolve("log.txt");
		Files.writeString(testFile, "Build completed successfully at 10:30 AM");

		FileContentJudge judge = new FileContentJudge("log.txt", "successfully", MatchMode.CONTAINS);
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.reasoning()).contains("contains").contains("matches");
	}

	@Test
	void containsMatchFailsWhenContentMissing() throws IOException {
		Path testFile = tempDir.resolve("log.txt");
		Files.writeString(testFile, "Build failed");

		FileContentJudge judge = new FileContentJudge("log.txt", "successfully", MatchMode.CONTAINS);
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isFalse();
	}

	@Test
	void regexMatchPassesWhenPatternMatches() throws IOException {
		Path testFile = tempDir.resolve("data.json");
		Files.writeString(testFile, "{\"status\": \"success\", \"count\": 42}");

		FileContentJudge judge = new FileContentJudge("data.json", "\\{.*\"status\".*\\}", MatchMode.REGEX);
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.reasoning()).contains("regex").contains("matches");
	}

	@Test
	void regexMatchFailsWhenPatternDoesNotMatch() throws IOException {
		Path testFile = tempDir.resolve("data.txt");
		Files.writeString(testFile, "plain text");

		FileContentJudge judge = new FileContentJudge("data.txt", "^\\d+$", MatchMode.REGEX);
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isFalse();
	}

	@Test
	void failsWhenFileDoesNotExist() {
		FileContentJudge judge = new FileContentJudge("missing.txt", "content", MatchMode.EXACT);
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isFalse();
		assertThat(judgment.reasoning()).contains("not found");
		assertThat(judgment.checks()).hasSize(1);
		assertThat(judgment.checks().get(0).name()).isEqualTo("file_exists");
		assertThat(judgment.checks().get(0).passed()).isFalse();
	}

	@Test
	void defaultsToExactMatch() throws IOException {
		Path testFile = tempDir.resolve("test.txt");
		Files.writeString(testFile, "exact");

		FileContentJudge judge = new FileContentJudge("test.txt", "exact");
		Judgment judgment = judge.judge(createContext());

		assertThat(judgment.pass()).isTrue();
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
