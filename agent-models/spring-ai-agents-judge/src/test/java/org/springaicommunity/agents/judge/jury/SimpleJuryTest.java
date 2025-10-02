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

package org.springaicommunity.agents.judge.jury;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.judge.Judge;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.result.JudgmentStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springaicommunity.agents.judge.JudgeTestFixtures.*;

/**
 * Tests for {@link SimpleJury}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class SimpleJuryTest {

	@Test
	void shouldExecuteJudgesInParallel() {
		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("Judge1"))
			.judge(alwaysPass("Judge2"))
			.judge(alwaysPass("Judge3"))
			.votingStrategy(new MajorityVotingStrategy())
			.parallel(true)
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.individual()).hasSize(3);
	}

	@Test
	void shouldExecuteJudgesSequentially() {
		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("Judge1"))
			.judge(alwaysFail("Judge2"))
			.judge(alwaysPass("Judge3"))
			.votingStrategy(new MajorityVotingStrategy())
			.parallel(false)
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.individual()).hasSize(3);
	}

	@Test
	void shouldPreserveJudgeIdentityByName() {
		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("FileExists"))
			.judge(alwaysFail("Correctness"))
			.judge(alwaysPass("BuildSuccess"))
			.votingStrategy(new MajorityVotingStrategy())
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.individualByName()).containsKeys("FileExists", "Correctness", "BuildSuccess");
		assertThat(verdict.individualByName().get("FileExists").status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.individualByName().get("Correctness").status()).isEqualTo(JudgmentStatus.FAIL);
	}

	@Test
	void shouldGenerateDefaultNamesForUnnamedJudges() {
		Judge unnamedJudge1 = ctx -> Judgment.pass("Pass 1");
		Judge unnamedJudge2 = ctx -> Judgment.fail("Fail 2");

		SimpleJury jury = SimpleJury.builder()
			.judge(unnamedJudge1)
			.judge(unnamedJudge2)
			.votingStrategy(new MajorityVotingStrategy())
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.individualByName()).containsKeys("Judge#1", "Judge#2");
	}

	@Test
	void shouldUseWeightedVoting() {
		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("Judge1"), 0.3)
			.judge(alwaysFail("Judge2"), 0.7)
			.votingStrategy(new WeightedAverageStrategy())
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		// Weighted: (1.0 * 0.3 + 0.0 * 0.7) / 1.0 = 0.3 < 0.5 → FAIL
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(verdict.weights()).containsEntry("0", 0.3).containsEntry("1", 0.7);
	}

	@Test
	void shouldUseCustomExecutor() {
		var executor = Executors.newFixedThreadPool(2);

		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("Judge1"))
			.judge(alwaysPass("Judge2"))
			.votingStrategy(new MajorityVotingStrategy())
			.parallel(true)
			.executor(executor)
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);

		executor.shutdown();
	}

	@Test
	void shouldReturnJudgesList() {
		Judge judge1 = alwaysPass("Judge1");
		Judge judge2 = alwaysFail("Judge2");

		SimpleJury jury = SimpleJury.builder()
			.judge(judge1)
			.judge(judge2)
			.votingStrategy(new MajorityVotingStrategy())
			.build();

		assertThat(jury.getJudges()).containsExactly(judge1, judge2);
	}

	@Test
	void shouldReturnVotingStrategy() {
		VotingStrategy strategy = new MajorityVotingStrategy();

		SimpleJury jury = SimpleJury.builder().judge(alwaysPass("Judge1")).votingStrategy(strategy).build();

		assertThat(jury.getVotingStrategy()).isEqualTo(strategy);
	}

	@Test
	void shouldPreserveOrderInIndividualByName() {
		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("First"))
			.judge(alwaysFail("Second"))
			.judge(alwaysPass("Third"))
			.votingStrategy(new MajorityVotingStrategy())
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		// LinkedHashMap preserves insertion order
		assertThat(verdict.individualByName()).containsKeys("First", "Second", "Third");
		assertThat(verdict.individualByName()).hasSize(3);
	}

	// ==================== Builder Tests ====================

	@Test
	void builderShouldRequireVotingStrategy() {
		assertThatThrownBy(() -> SimpleJury.builder().judge(alwaysPass("Judge1")).build())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Voting strategy is required");
	}

	@Test
	void builderShouldRequireAtLeastOneJudge() {
		assertThatThrownBy(() -> SimpleJury.builder().votingStrategy(new MajorityVotingStrategy()).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("at least one judge");
	}

	@Test
	void builderShouldRejectNullJudge() {
		assertThatThrownBy(() -> SimpleJury.builder().judge(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Judge cannot be null");
	}

	@Test
	void builderShouldRejectNegativeWeight() {
		assertThatThrownBy(() -> SimpleJury.builder().judge(alwaysPass("Judge1"), -1.0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("non-negative");
	}

	@Test
	void builderShouldAcceptZeroWeight() {
		// Zero weight is valid - judge participates but with no influence
		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("Judge1"), 0.0)
			.judge(alwaysFail("Judge2"), 1.0)
			.votingStrategy(new WeightedAverageStrategy())
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		// Only Judge2 has weight → result should be FAIL
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.FAIL);
	}

	@Test
	void builderShouldDefaultToParallelTrue() {
		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("Judge1"))
			.votingStrategy(new MajorityVotingStrategy())
			.build();

		// No direct way to test parallel flag, but verify it works
		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
	}

	@Test
	void builderShouldSupportJudgeWithoutWeight() {
		SimpleJury jury = SimpleJury.builder()
			.judge(alwaysPass("Judge1")) // defaults to weight 1.0
			.judge(alwaysFail("Judge2")) // defaults to weight 1.0
			.votingStrategy(new WeightedAverageStrategy())
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		// Equal weights: (1.0 * 1.0 + 0.0 * 1.0) / 2.0 = 0.5 → PASS
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
	}

	// ==================== Edge Cases ====================

	@Test
	void shouldHandleRecordingJudges() {
		var recording = recording("RecordingJudge", booleanPass("Recorded"));

		SimpleJury jury = SimpleJury.builder()
			.judge(recording)
			.judge(alwaysPass("Judge2"))
			.votingStrategy(new MajorityVotingStrategy())
			.build();

		JudgmentContext context = simpleContext("Test goal");
		jury.vote(context);

		assertThat(recording.getInvocationCount()).isEqualTo(1);
		assertThat(recording.getLastInvocation()).isEqualTo(context);
	}

	@Test
	void shouldHandleSlowJudgesWithTimeout() {
		SimpleJury jury = SimpleJury.builder()
			.judge(slow("SlowJudge", 100, booleanPass("Slow pass")))
			.judge(alwaysPass("FastJudge"))
			.votingStrategy(new MajorityVotingStrategy())
			.parallel(true)
			.build();

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		// Both should complete
		assertThat(verdict.individual()).hasSize(2);
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
	}

}
