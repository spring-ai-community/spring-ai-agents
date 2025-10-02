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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.judge.JudgeType;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.score.BooleanScore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CorrectnessJudge parsing logic.
 *
 * Note: These tests focus on the prompt building and response parsing logic. Integration
 * tests with real LLM are in CorrectnessJudgeIT.
 *
 * @author Mark Pollack
 */
class CorrectnessJudgeTest {

	@Test
	void hasCorrectMetadata() {
		// Create a test subclass to avoid needing ChatClient
		TestCorrectnessJudge judge = new TestCorrectnessJudge();

		assertThat(judge.metadata().name()).isEqualTo("Correctness");
		assertThat(judge.metadata().description()).isEqualTo("Evaluates if agent accomplished the goal");
		assertThat(judge.metadata().type()).isEqualTo(JudgeType.LLM_POWERED);
	}

	@Test
	void buildsPromptWithAllContext() {
		TestCorrectnessJudge judge = new TestCorrectnessJudge();

		JudgmentContext context = JudgmentContext.builder()
			.goal("Create hello.txt")
			.workspace(Path.of("/tmp"))
			.agentOutput("File created successfully")
			.build();

		String prompt = judge.testBuildPrompt(context);

		assertThat(prompt).contains("Goal: Create hello.txt");
		assertThat(prompt).contains("Workspace: /tmp");
		assertThat(prompt).contains("Agent Output: File created successfully");
		assertThat(prompt).contains("Did the agent accomplish the goal?");
	}

	@Test
	void parsesStructuredYesResponse() {
		TestCorrectnessJudge judge = new TestCorrectnessJudge();

		String response = """
				Answer: YES
				Reasoning: The agent successfully created the file with correct content.
				""";

		Judgment judgment = judge.testParseResponse(response, null);

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.score()).isInstanceOf(BooleanScore.class);
		assertThat(((BooleanScore) judgment.score()).value()).isTrue();
		assertThat(judgment.reasoning()).contains("successfully created the file");
	}

	@Test
	void parsesStructuredNoResponse() {
		TestCorrectnessJudge judge = new TestCorrectnessJudge();

		String response = """
				Answer: NO
				Reasoning: The file was not created in the expected location.
				""";

		Judgment judgment = judge.testParseResponse(response, null);

		assertThat(judgment.pass()).isFalse();
		assertThat(judgment.score()).isInstanceOf(BooleanScore.class);
		assertThat(((BooleanScore) judgment.score()).value()).isFalse();
		assertThat(judgment.reasoning()).contains("file was not created");
	}

	@Test
	void parsesUnstructuredYesResponse() {
		TestCorrectnessJudge judge = new TestCorrectnessJudge();

		String response = """
				YES - The agent did accomplish the goal.
				The output shows that the README file was created with all required sections.
				""";

		Judgment judgment = judge.testParseResponse(response, null);

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.reasoning()).contains("README file was created");
	}

	@Test
	void parsesUnstructuredNoResponse() {
		TestCorrectnessJudge judge = new TestCorrectnessJudge();

		String response = """
				NO
				The agent failed to complete the task because the build process encountered errors.
				""";

		Judgment judgment = judge.testParseResponse(response, null);

		assertThat(judgment.pass()).isFalse();
		assertThat(judgment.reasoning()).contains("build process encountered errors");
	}

	@Test
	void fallbackToFullResponseWhenNoStructure() {
		TestCorrectnessJudge judge = new TestCorrectnessJudge();

		String response = "The task was completed successfully without any issues.";

		Judgment judgment = judge.testParseResponse(response, null);

		// When response doesn't have clear YES/NO but mentions success
		assertThat(judgment.reasoning()).isNotEmpty();
		assertThat(judgment.reasoning()).isEqualTo(response);
	}

	// Test subclass that exposes protected methods for testing
	static class TestCorrectnessJudge extends CorrectnessJudge {

		public TestCorrectnessJudge() {
			// Use null builder - we won't call judge() in unit tests
			super(null);
		}

		public String testBuildPrompt(JudgmentContext context) {
			return buildPrompt(context);
		}

		public Judgment testParseResponse(String response, JudgmentContext context) {
			return parseResponse(response, context);
		}

	}

}
