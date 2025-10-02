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
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.result.JudgmentStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springaicommunity.agents.judge.JudgeTestFixtures.*;

/**
 * Tests for {@link MajorityVotingStrategy}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class MajorityVotingStrategyTest {

	@Test
	void shouldReturnPassWhenPassesOutnumberFails() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(booleanPass("Judge 1 passed"), booleanPass("Judge 2 passed"),
				booleanFail("Judge 3 failed"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(result.reasoning()).contains("2 passed");
	}

	@Test
	void shouldReturnFailWhenFailsOutnumberPasses() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.PASS, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(booleanPass("Judge 1 passed"), booleanFail("Judge 2 failed"),
				booleanFail("Judge 3 failed"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(result.reasoning()).contains("2 failed");
	}

	// ==================== TiePolicy Tests ====================

	@Test
	void tieShouldUsePassPolicyWhenConfigured() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.PASS, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), booleanFail("Judge 2"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(result.reasoning()).contains("tie");
	}

	@Test
	void tieShouldUseFailPolicyWhenConfigured() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), booleanFail("Judge 2"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(result.reasoning()).contains("tie");
	}

	@Test
	void tieShouldUseAbstainPolicyWhenConfigured() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.ABSTAIN, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), booleanFail("Judge 2"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.ABSTAIN);
		assertThat(result.reasoning()).contains("tie");
	}

	// ==================== ErrorPolicy Tests ====================

	@Test
	void allErrorsShouldTreatAsFailWhenConfigured() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(Judgment.error("Error 1", new RuntimeException()),
				Judgment.error("Error 2", new RuntimeException()));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(result.reasoning()).contains("Majority vote");
		assertThat(result.reasoning()).contains("2 failed");
	}

	@Test
	void allErrorsShouldTreatAsAbstainWhenConfigured() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_ABSTAIN);

		List<Judgment> judgments = List.of(Judgment.error("Error 1", new RuntimeException()),
				Judgment.error("Error 2", new RuntimeException()));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.ABSTAIN);
		assertThat(result.reasoning()).contains("All judges abstained");
	}

	@Test
	void allErrorsShouldBeIgnoredWhenConfigured() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.IGNORE);

		List<Judgment> judgments = List.of(Judgment.error("Error 1", new RuntimeException()),
				Judgment.error("Error 2", new RuntimeException()));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// All ignored → no valid judgments → ABSTAIN
		assertThat(result.status()).isEqualTo(JudgmentStatus.ABSTAIN);
		assertThat(result.reasoning()).contains("All judges abstained");
	}

	@Test
	void mixedErrorsAndPassesShouldRespectErrorPolicy() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), Judgment.error("Error", new RuntimeException()),
				booleanPass("Judge 3"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// 2 passes + 1 error-as-fail = 2 pass vs 1 fail
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
	}

	// ==================== Abstain Tests ====================

	@Test
	void allAbstainsShouldReturnAbstain() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(Judgment.abstain("Cannot evaluate 1"),
				Judgment.abstain("Cannot evaluate 2"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.ABSTAIN);
		assertThat(result.reasoning()).contains("All judges abstained");
	}

	@Test
	void abstainsShouldNotCountInMajority() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), Judgment.abstain("Judge 2"), booleanFail("Judge 3"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// 1 pass, 1 fail, 1 abstain → tie between pass/fail
		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL); // TiePolicy.FAIL
	}

	// ==================== Mixed Scenarios ====================

	@Test
	void mixedPassFailAbstainError() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.ABSTAIN, ErrorPolicy.IGNORE);

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), booleanFail("Judge 2"), Judgment.abstain("Judge 3"),
				Judgment.error("Judge 4", new RuntimeException()));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// 1 pass, 1 fail, 1 abstain (not counted), 1 error (ignored) = 1 pass vs 1
		// fail → tie
		assertThat(result.status()).isEqualTo(JudgmentStatus.ABSTAIN); // TiePolicy.ABSTAIN
	}

	// ==================== Edge Cases ====================

	@Test
	void emptyJudgmentListShouldThrowException() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		assertThatThrownBy(() -> strategy.aggregate(List.of(), Map.of())).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("empty");
	}

	@Test
	void singleJudgmentShouldUseItsStatus() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		Judgment result = strategy.aggregate(List.of(booleanPass("Only judge")), Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
	}

	// ==================== Metadata Tests ====================

	@Test
	void shouldReturnCorrectName() {
		MajorityVotingStrategy strategy = new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);

		assertThat(strategy.getName()).isEqualTo("majority");
	}

}
