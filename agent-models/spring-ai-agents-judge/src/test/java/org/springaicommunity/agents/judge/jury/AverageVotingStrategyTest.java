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
import org.springaicommunity.agents.judge.score.NumericalScore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springaicommunity.agents.judge.JudgeTestFixtures.*;

/**
 * Tests for {@link AverageVotingStrategy}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class AverageVotingStrategyTest {

	@Test
	void shouldCalculateAverageAboveThreshold() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), passJudgment(0.7), passJudgment(0.6));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(result.score()).isInstanceOf(NumericalScore.class);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.7, org.assertj.core.data.Offset.offset(0.01)); // (0.8
																									// +
																									// 0.7
																									// +
																									// 0.6)
																									// /
																									// 3
		assertThat(result.reasoning()).contains("Average score: 0.70");
	}

	@Test
	void shouldCalculateAverageBelowThreshold() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		List<Judgment> judgments = List.of(failJudgment(0.3), failJudgment(0.2), failJudgment(0.1));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(result.score()).isInstanceOf(NumericalScore.class);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.2, org.assertj.core.data.Offset.offset(0.01)); // (0.3
																									// +
																									// 0.2
																									// +
																									// 0.1)
																									// /
																									// 3
		assertThat(result.reasoning()).contains("Average score: 0.20");
	}

	@Test
	void shouldTreatBooleanAsNumerical() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), // 1.0
				booleanFail("Judge 2"), // 0.0
				booleanPass("Judge 3") // 1.0
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// (1.0 + 0.0 + 1.0) / 3 = 0.67 > 0.5 → PASS
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.67, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldHandleMixedBooleanAndNumerical() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), // 1.0
				passJudgment(0.6), // 0.6
				booleanFail("Judge 3") // 0.0
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// (1.0 + 0.6 + 0.0) / 3 = 0.53 > 0.5 → PASS
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.53, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldHandleExactThreshold() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		// Construct judgments that average exactly to 0.5
		List<Judgment> judgments = List.of(booleanPass("Judge 1"), // 1.0
				booleanFail("Judge 2") // 0.0
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// (1.0 + 0.0) / 2 = 0.5 >= 0.5 → PASS
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.5);
	}

	@Test
	void shouldHandleNullScoreAsZero() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		// Create judgments with null scores (ABSTAIN/ERROR status)
		List<Judgment> judgments = List.of(passJudgment(0.8), Judgment.abstain("Cannot evaluate"), // null
																									// score
																									// →
																									// 0.0
				passJudgment(0.6));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// (0.8 + 0.0 + 0.6) / 3 = 0.47 < 0.5 → FAIL
		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
	}

	@Test
	void shouldIgnoreWeightsParameter() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), passJudgment(0.6));

		// Weights are ignored in AverageVotingStrategy
		Map<String, Double> weights = Map.of("Judge1", 2.0, "Judge2", 1.0);

		Judgment result = strategy.aggregate(judgments, weights);

		// Simple average: (0.8 + 0.6) / 2 = 0.7 (weights ignored)
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.7);
	}

	// ==================== Edge Cases ====================

	@Test
	void emptyJudgmentListShouldThrowException() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		assertThatThrownBy(() -> strategy.aggregate(List.of(), Map.of())).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("empty");
	}

	@Test
	void nullJudgmentListShouldThrowException() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		assertThatThrownBy(() -> strategy.aggregate(null, Map.of())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void singleJudgmentShouldReturnItsScore() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		Judgment result = strategy.aggregate(List.of(passJudgment(0.8)), Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.8);
	}

	@Test
	void allZeroScoresShouldFail() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		List<Judgment> judgments = List.of(failJudgment(0.0), failJudgment(0.0));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.0);
	}

	@Test
	void allOneScoresShouldPass() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(1.0), passJudgment(1.0));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(1.0);
	}

	// ==================== Metadata Tests ====================

	@Test
	void shouldReturnCorrectName() {
		AverageVotingStrategy strategy = new AverageVotingStrategy();

		assertThat(strategy.getName()).isEqualTo("AverageVoting");
	}

}
