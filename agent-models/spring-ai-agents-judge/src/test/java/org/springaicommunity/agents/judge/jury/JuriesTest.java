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
import org.springaicommunity.agents.judge.result.JudgmentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springaicommunity.agents.judge.JudgeTestFixtures.*;

/**
 * Tests for {@link Juries} utility class.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class JuriesTest {

	@Test
	void fromJudgesShouldCreateJuryWithAutoNaming() {
		Judge unnamedJudge1 = ctx -> booleanPass("Pass 1");
		Judge unnamedJudge2 = ctx -> booleanFail("Fail 2");

		Jury jury = Juries.fromJudges(new MajorityVotingStrategy(), unnamedJudge1, unnamedJudge2);

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.individualByName()).containsKeys("Judge#1", "Judge#2");
	}

	@Test
	void fromJudgesShouldPreserveNamedJudges() {
		Judge named1 = alwaysPass("FileExists");
		Judge named2 = alwaysFail("Correctness");
		Judge named3 = alwaysPass("BuildSuccess");

		Jury jury = Juries.fromJudges(new MajorityVotingStrategy(), named1, named2, named3);

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.individualByName()).containsKeys("FileExists", "Correctness", "BuildSuccess");
	}

	@Test
	void fromJudgesShouldHandleDuplicateNamesWithSuffixes() {
		Judge judge1 = alwaysPass("FileCheck");
		Judge judge2 = alwaysFail("FileCheck"); // duplicate name
		Judge judge3 = alwaysPass("FileCheck"); // another duplicate

		Jury jury = Juries.fromJudges(new MajorityVotingStrategy(), judge1, judge2, judge3);

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		// First keeps original, duplicates get -2, -3 suffixes
		assertThat(verdict.individualByName()).containsKeys("FileCheck", "FileCheck-2", "FileCheck-3");
	}

	@Test
	void fromJudgesShouldHandleMixedNamedAndUnnamed() {
		Judge named = alwaysPass("NamedJudge");
		Judge unnamed = ctx -> booleanFail("Unnamed");

		Jury jury = Juries.fromJudges(new MajorityVotingStrategy(), named, unnamed);

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.individualByName()).containsKeys("NamedJudge", "Judge#2");
	}

	@Test
	void fromJudgesShouldRequireAtLeastOneJudge() {
		assertThatThrownBy(() -> Juries.fromJudges(new MajorityVotingStrategy()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("At least one judge is required");
	}

	@Test
	void fromJudgesShouldRejectNullJudges() {
		assertThatThrownBy(() -> Juries.fromJudges(new MajorityVotingStrategy(), (Judge[]) null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	// ==================== combine() Tests ====================

	@Test
	void combineShouldCreateMetaJury() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"), alwaysPass("J2"));

		Jury jury2 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysFail("J3"), alwaysFail("J4"));

		Jury metaJury = Juries.combine(jury1, jury2, new MajorityVotingStrategy());

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// jury1 → PASS, jury2 → FAIL, majority → FAIL (tie resolved by TiePolicy)
		assertThat(verdict.subVerdicts()).hasSize(2);
	}

	@Test
	void combineShouldRequireNonNullJuries() {
		Jury jury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"));

		assertThatThrownBy(() -> Juries.combine(null, jury, new MajorityVotingStrategy()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Both juries must be non-null");

		assertThatThrownBy(() -> Juries.combine(jury, null, new MajorityVotingStrategy()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Both juries must be non-null");
	}

	// ==================== allOf() Tests ====================

	@Test
	void allOfShouldCreateMetaJuryFromMultiple() {
		Jury jury1 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J1"));
		Jury jury2 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J2"));
		Jury jury3 = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("J3"));

		Jury metaJury = Juries.allOf(new ConsensusStrategy(), jury1, jury2, jury3);

		JudgmentContext context = simpleContext("Test goal");
		Verdict verdict = metaJury.vote(context);

		// All juries pass → consensus pass
		assertThat(verdict.subVerdicts()).hasSize(3);
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
	}

	@Test
	void allOfShouldRequireAtLeastOneJury() {
		assertThatThrownBy(() -> Juries.allOf(new ConsensusStrategy())).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("At least one jury is required");
	}

	@Test
	void allOfShouldRejectNullJuries() {
		assertThatThrownBy(() -> Juries.allOf(new ConsensusStrategy(), (Jury[]) null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	// ==================== Integration Tests ====================

	@Test
	void shouldSupportComplexJuryComposition() {
		// Create specialized juries
		Jury fileJury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("FileExists"),
				alwaysPass("FileContent"));

		Jury buildJury = Juries.fromJudges(new MajorityVotingStrategy(), alwaysPass("MavenBuild"),
				alwaysPass("GradleBuild"));

		Jury correctnessJury = Juries.fromJudges(new ConsensusStrategy(), alwaysPass("Correctness1"),
				alwaysPass("Correctness2"));

		// Combine into meta-jury
		Jury metaJury = Juries.allOf(new MajorityVotingStrategy(), fileJury, buildJury, correctnessJury);

		JudgmentContext context = simpleContext("Complex evaluation");
		Verdict verdict = metaJury.vote(context);

		// All sub-juries pass → majority passes
		assertThat(verdict.subVerdicts()).hasSize(3);
		assertThat(verdict.aggregated().status()).isEqualTo(JudgmentStatus.PASS);
	}

	@Test
	void shouldPreserveJudgeIdentityThroughComposition() {
		Judge judge1 = alwaysPass("UniqueJudge1");
		Judge judge2 = alwaysFail("UniqueJudge2");

		Jury jury = Juries.fromJudges(new MajorityVotingStrategy(), judge1, judge2);

		JudgmentContext context = simpleContext("Test");
		Verdict verdict = jury.vote(context);

		assertThat(verdict.individualByName().get("UniqueJudge1").status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(verdict.individualByName().get("UniqueJudge2").status()).isEqualTo(JudgmentStatus.FAIL);
	}

}
