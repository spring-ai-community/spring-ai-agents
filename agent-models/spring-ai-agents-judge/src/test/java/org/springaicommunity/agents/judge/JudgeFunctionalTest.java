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

package org.springaicommunity.agents.judge;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests demonstrating Judge as a functional interface.
 *
 * @author Mark Pollack
 */
class JudgeFunctionalTest {

	@Test
	void lambdaJudgeWorks() {
		// Lambda judge - very simple
		Judge simplePass = ctx -> Judgment.pass("All good");

		JudgmentContext context = JudgmentContext.builder().goal("test").workspace(Path.of("/tmp")).build();

		Judgment judgment = simplePass.judge(context);

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.reasoning()).isEqualTo("All good");
	}

	@Test
	void namedJudgeHasMetadata() {
		// Lambda with metadata via Judges.named()
		Judge simple = ctx -> Judgment.pass("Success");

		NamedJudge named = Judges.named(simple, "MyJudge", "A test judge");

		assertThat(named.metadata().name()).isEqualTo("MyJudge");
		assertThat(named.metadata().description()).isEqualTo("A test judge");
		assertThat(named.metadata().type()).isEqualTo(JudgeType.DETERMINISTIC);
	}

	@Test
	void alwaysPassJudge() {
		Judge pass = Judges.alwaysPass("Default success");

		JudgmentContext context = JudgmentContext.builder().goal("test").workspace(Path.of("/tmp")).build();

		Judgment judgment = pass.judge(context);

		assertThat(judgment.pass()).isTrue();
		assertThat(judgment.reasoning()).isEqualTo("Default success");
	}

	@Test
	void alwaysFailJudge() {
		Judge fail = Judges.alwaysFail("Not implemented yet");

		JudgmentContext context = JudgmentContext.builder().goal("test").workspace(Path.of("/tmp")).build();

		Judgment judgment = fail.judge(context);

		assertThat(judgment.pass()).isFalse();
		assertThat(judgment.reasoning()).isEqualTo("Not implemented yet");
	}

	@Test
	void methodReferenceWorks() {
		// Method reference judge
		Judge methodRef = this::validateOutput;

		JudgmentContext context = JudgmentContext.builder()
			.goal("test")
			.workspace(Path.of("/tmp"))
			.agentOutput("valid output")
			.build();

		Judgment judgment = methodRef.judge(context);

		assertThat(judgment.pass()).isTrue();
	}

	// Method to use as reference
	private Judgment validateOutput(JudgmentContext ctx) {
		boolean valid = ctx.agentOutput().isPresent() && ctx.agentOutput().get().contains("valid");
		return valid ? Judgment.pass("Output valid") : Judgment.fail("Output invalid");
	}

}
