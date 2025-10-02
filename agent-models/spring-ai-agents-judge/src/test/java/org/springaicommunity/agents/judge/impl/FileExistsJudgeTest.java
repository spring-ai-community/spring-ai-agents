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
import org.springaicommunity.agents.judge.JudgeType;
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springaicommunity.agents.judge.score.ScoreType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FileExistsJudgeTest {

	@TempDir
	Path tempDir;

	@Test
	void passesWhenFileExists() throws IOException {
		// Create a test file
		Path testFile = tempDir.resolve("test.txt");
		Files.writeString(testFile, "test content");

		FileExistsJudge judge = new FileExistsJudge("test.txt");

		JudgmentContext context = JudgmentContext.builder()
			.goal("Create test.txt")
			.workspace(tempDir)
			.executionTime(Duration.ofSeconds(1))
			.startedAt(Instant.now())
			.status(AgentExecutionStatus.SUCCESS)
			.build();

		Judgment judgment = judge.judge(context);

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.score()).isInstanceOf(BooleanScore.class);
		assertThat(((BooleanScore) judgment.score()).value()).isTrue();
		assertThat(judgment.score().type()).isEqualTo(ScoreType.BOOLEAN);
		assertThat(judgment.reasoning()).contains("File exists");
		assertThat(judgment.checks()).hasSize(1);
		assertThat(judgment.checks().get(0).passed()).isTrue();
	}

	@Test
	void failsWhenFileDoesNotExist() {
		FileExistsJudge judge = new FileExistsJudge("nonexistent.txt");

		JudgmentContext context = JudgmentContext.builder()
			.goal("Create nonexistent.txt")
			.workspace(tempDir)
			.executionTime(Duration.ofSeconds(1))
			.startedAt(Instant.now())
			.status(AgentExecutionStatus.FAILED)
			.build();

		Judgment judgment = judge.judge(context);

		assertThat(judgment.pass()).isFalse();
		assertThat(judgment.score()).isInstanceOf(BooleanScore.class);
		assertThat(((BooleanScore) judgment.score()).value()).isFalse();
		assertThat(judgment.reasoning()).contains("File not found");
		assertThat(judgment.checks()).hasSize(1);
		assertThat(judgment.checks().get(0).passed()).isFalse();
	}

	@Test
	void hasCorrectMetadata() {
		FileExistsJudge judge = new FileExistsJudge("test.txt");

		assertThat(judge.metadata().name()).isEqualTo("FileExistsJudge");
		assertThat(judge.metadata().description()).contains("test.txt");
		assertThat(judge.metadata().type()).isEqualTo(JudgeType.DETERMINISTIC);
	}

}
