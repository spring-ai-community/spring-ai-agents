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
import org.springaicommunity.agents.judge.score.BooleanScore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springaicommunity.agents.judge.JudgeTestFixtures.*;

/**
 * Tests for {@link ConsensusStrategy}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class ConsensusStrategyTest {

	@Test
	void shouldPassWhenUnanimousPass() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), booleanPass("Judge 2"), booleanPass("Judge 3"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(result.score()).isInstanceOf(BooleanScore.class);
		BooleanScore score = (BooleanScore) result.score();
		assertThat(score.value()).isTrue();
		assertThat(result.reasoning()).contains("Unanimous consensus");
		assertThat(result.reasoning()).contains("all 3 judges passed");
	}

	@Test
	void shouldFailWhenUnanimousFail() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(booleanFail("Judge 1"), booleanFail("Judge 2"), booleanFail("Judge 3"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		BooleanScore score = (BooleanScore) result.score();
		assertThat(score.value()).isFalse();
		assertThat(result.reasoning()).contains("Unanimous consensus");
		assertThat(result.reasoning()).contains("all 3 judges failed");
	}

	@Test
	void shouldFailWhenNoConsensus() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), booleanPass("Judge 2"), booleanFail("Judge 3"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		BooleanScore score = (BooleanScore) result.score();
		assertThat(score.value()).isFalse();
		assertThat(result.reasoning()).contains("No consensus");
		assertThat(result.reasoning()).contains("2 passed, 1 failed");
	}

	@Test
	void shouldHandleSingleJudge() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		Judgment result = strategy.aggregate(List.of(booleanPass("Only judge")), Map.of());

		// Single judge → unanimous → pass
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(result.reasoning()).contains("all 1 judges passed");
	}

	@Test
	void shouldConvertNumericalScoresToBoolean() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), // >= 0.5 → pass
				passJudgment(0.9), // >= 0.5 → pass
				passJudgment(0.6) // >= 0.5 → pass
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// All numerical scores >= 0.5 → unanimous pass
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
	}

	@Test
	void shouldTreatNumericalBelowThresholdAsFail() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(failJudgment(0.3), // < 0.5 → fail
				failJudgment(0.2), // < 0.5 → fail
				failJudgment(0.1) // < 0.5 → fail
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// All numerical scores < 0.5 → unanimous fail
		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(result.reasoning()).contains("all 3 judges failed");
	}

	@Test
	void shouldHandleMixedNumericalAndBoolean() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), // pass
				passJudgment(0.8), // >= 0.5 → pass
				booleanPass("Judge 3") // pass
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// All convert to pass → unanimous
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
	}

	@Test
	void shouldFailWhenMixedNumericalAndBooleanDisagree() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), // pass
				failJudgment(0.3), // < 0.5 → fail
				booleanPass("Judge 3") // pass
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Mixed → no consensus → fail
		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(result.reasoning()).contains("No consensus");
		assertThat(result.reasoning()).contains("2 passed, 1 failed");
	}

	@Test
	void shouldHandleExactThresholdAsPass() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.5), // exactly 0.5 → pass
				passJudgment(0.5));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// 0.5 >= 0.5 → pass for both → unanimous
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
	}

	@Test
	void shouldHandleNullScoreAsFail() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), Judgment.abstain("Cannot evaluate"), // null
																										// score
																										// →
																										// false
				booleanPass("Judge 3"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// 2 pass, 1 null→fail → no consensus
		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(result.reasoning()).contains("No consensus");
	}

	@Test
	void shouldIgnoreWeightsParameter() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), booleanPass("Judge 2"));

		// Weights are ignored in ConsensusStrategy
		Map<String, Double> weights = Map.of("0", 100.0, "1", 1.0);

		Judgment result = strategy.aggregate(judgments, weights);

		// Unanimous pass regardless of weights
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
	}

	// ==================== Edge Cases ====================

	@Test
	void emptyJudgmentListShouldThrowException() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		assertThatThrownBy(() -> strategy.aggregate(List.of(), Map.of())).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("empty");
	}

	@Test
	void nullJudgmentListShouldThrowException() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		assertThatThrownBy(() -> strategy.aggregate(null, Map.of())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void oneMajorityIsNotConsensus() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		// 2 pass, 1 fail → no consensus
		List<Judgment> judgments = List.of(booleanPass("Judge 1"), booleanPass("Judge 2"), booleanFail("Judge 3"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Consensus requires ALL to agree
		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
	}

	@Test
	void oneMajorityFailIsNotConsensus() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		// 2 fail, 1 pass → no consensus
		List<Judgment> judgments = List.of(booleanFail("Judge 1"), booleanFail("Judge 2"), booleanPass("Judge 3"));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(result.reasoning()).contains("No consensus");
	}

	// ==================== Metadata Tests ====================

	@Test
	void shouldReturnCorrectName() {
		ConsensusStrategy strategy = new ConsensusStrategy();

		assertThat(strategy.getName()).isEqualTo("Consensus");
	}

}
