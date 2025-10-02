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
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.JudgmentStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springaicommunity.agents.judge.JudgeTestFixtures.*;

/**
 * Tests for {@link MetaJury}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class MetaJuryTest {

	@Test
	void shouldAggregateSubJuryVerdicts() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"), alwaysPass("J2"));

		Jury jury2 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J3"), alwaysPass("J4"));

		MetaJury metaJury = new MetaJury(List.of(jury1, jury2), new MajorityVotingStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// Both juries pass → meta passes
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.subVerdicts()).hasSize(2);
		assertThat(verdict.individual()).hasSize(2);
	}

	@Test
	void shouldPreserveSubVerdictsInFinalVerdict() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("FileCheck"), alwaysPass("Build"));

		Jury jury2 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysFail("Correctness"), alwaysFail("Security"));

		MetaJury metaJury = new MetaJury(List.of(jury1, jury2), new MajorityVotingStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// Verify sub-verdicts are preserved
		assertThat(verdict.subVerdicts()).hasSize(2);

		Verdict subVerdict1 = verdict.subVerdicts().get(0);
		assertThat(subVerdict1.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(subVerdict1.individual()).hasSize(2);

		Verdict subVerdict2 = verdict.subVerdicts().get(1);
		assertThat(subVerdict2.aggregated().status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(subVerdict2.individual()).hasSize(2);
	}

	@Test
	void shouldAggregateWithMixedJuryResults() {
		Jury passJury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("P1"), alwaysPass("P2"));

		Jury failJury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysFail("F1"), alwaysFail("F2"));

		MetaJury metaJury = new MetaJury(List.of(passJury, failJury), new MajorityVotingStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// One pass, one fail → tie → depends on TiePolicy (default FAIL)
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(verdict.individual()).hasSize(2);
		assertThat(verdict.individual().get(0).status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.individual().get(1).status()).isEqualTo(JudgmentStatus.FAIL);
	}

	@Test
	void shouldHandleSingleJury() {
		Jury singleJury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"), alwaysFail("J2"),
				alwaysPass("J3"));

		MetaJury metaJury = new MetaJury(List.of(singleJury), new ConsensusStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// Single jury verdict → meta result is same
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.subVerdicts()).hasSize(1);
	}

	@Test
	void shouldReturnEmptyJudgesList() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"));

		MetaJury metaJury = new MetaJury(List.of(jury1), new MajorityVotingStrategy());

		// Meta-jury doesn't expose judges directly, only sub-juries
		assertThat(metaJury.getJudges()).isEmpty();
	}

	@Test
	void shouldReturnMetaVotingStrategy() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"));

		VotingStrategy metaStrategy = new WeightedAverageStrategy();
		MetaJury metaJury = new MetaJury(List.of(jury1), metaStrategy);

		assertThat(metaJury.getVotingStrategy()).isEqualTo(metaStrategy);
	}

	@Test
	void shouldUseWeightedAverageForMetaAggregation() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"), alwaysPass("J2"));

		Jury jury2 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysFail("J3"), alwaysFail("J4"));

		// Use weighted average - equal weights should give 0.5
		MetaJury metaJury = new MetaJury(List.of(jury1, jury2), new WeightedAverageStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// (1.0 + 0.0) / 2 = 0.5 → PASS (threshold is >= 0.5)
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
	}

	@Test
	void shouldUseConsensusForMetaAggregation() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"), alwaysPass("J2"));

		Jury jury2 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J3"), alwaysPass("J4"));

		Jury jury3 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J5"), alwaysPass("J6"));

		MetaJury metaJury = new MetaJury(List.of(jury1, jury2, jury3), new ConsensusStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// All juries pass → consensus passes
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.subVerdicts()).hasSize(3);
	}

	@Test
	void shouldFailConsensusWithSingleFailure() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"), alwaysPass("J2"));

		Jury jury2 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysFail("J3"), alwaysFail("J4"));

		MetaJury metaJury = new MetaJury(List.of(jury1, jury2), new ConsensusStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// One failure breaks consensus
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.FAIL);
	}

	// ==================== Constructor Validation Tests ====================

	@Test
	void shouldRejectNullJuries() {
		assertThatThrownBy(() -> new MetaJury(null, new MajorityVotingStrategy()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("At least one jury is required");
	}

	@Test
	void shouldRejectEmptyJuries() {
		assertThatThrownBy(() -> new MetaJury(List.of(), new MajorityVotingStrategy()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("At least one jury is required");
	}

	@Test
	void shouldRejectNullMetaStrategy() {
		Jury jury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"));

		assertThatThrownBy(() -> new MetaJury(List.of(jury), null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Meta voting strategy is required");
	}

	// ==================== Integration Tests ====================

	@Test
	void shouldSupportNestedMetaJuries() {
		// Create base juries
		Jury fileJury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("FileExists"),
				alwaysPass("FileContent"));

		Jury buildJury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("MavenBuild"),
				alwaysPass("GradleBuild"));

		// Create first-level meta-jury
		MetaJury infraJury = new MetaJury(List.of(fileJury, buildJury), new ConsensusStrategy());

		// Create correctness jury
		Jury correctnessJury = Juries.fromJudges(new ConsensusStrategy(), alwaysPass("Correctness1"),
				alwaysPass("Correctness2"));

		// Create second-level meta-jury (jury of juries of juries)
		MetaJury topLevelJury = new MetaJury(List.of(infraJury, correctnessJury), new MajorityVotingStrategy());

		JudgmentContext context = simpleContext("Complex nested evaluation");
		Verdict verdict = topLevelJury.vote(context);

		// All pass → top level passes
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.subVerdicts()).hasSize(2);

		// First sub-verdict is from infraJury (which has 2 sub-verdicts)
		Verdict infraVerdict = verdict.subVerdicts().get(0);
		assertThat(infraVerdict.subVerdicts()).hasSize(2);
	}

	@Test
	void shouldPreserveIndividualJudgmentsFromEachJury() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"), alwaysPass("J2"));

		Jury jury2 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysFail("J3"));

		MetaJury metaJury = new MetaJury(List.of(jury1, jury2), new MajorityVotingStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// Individual list contains aggregated judgments from each jury
		assertThat(verdict.individual()).hasSize(2);
		assertThat(verdict.individual().get(0).status()).isEqualTo(JudgmentStatus.PASS); // jury1
		assertThat(verdict.individual().get(1).status()).isEqualTo(JudgmentStatus.FAIL); // jury2
	}

}
