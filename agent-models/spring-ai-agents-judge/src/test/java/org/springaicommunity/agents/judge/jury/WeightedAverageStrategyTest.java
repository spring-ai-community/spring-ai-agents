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
 * Tests for {@link WeightedAverageStrategy}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class WeightedAverageStrategyTest {

	@Test
	void shouldCalculateWeightedAverage() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), // weight 0.3
				passJudgment(0.6) // weight 0.7
		);

		Map<String, Double> weights = Map.of("0", 0.3, "1", 0.7);

		Judgment result = strategy.aggregate(judgments, weights);

		// (0.8 * 0.3 + 0.6 * 0.7) / (0.3 + 0.7) = (0.24 + 0.42) / 1.0 = 0.66
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(result.score()).isInstanceOf(NumericalScore.class);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.66, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldNormalizeWeights() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), passJudgment(0.6));

		// Weights don't sum to 1.0 - should be normalized
		Map<String, Double> weights = Map.of("0", 3.0, "1", 7.0);

		Judgment result = strategy.aggregate(judgments, weights);

		// (0.8 * 3.0 + 0.6 * 7.0) / (3.0 + 7.0) = (2.4 + 4.2) / 10.0 = 0.66
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.66, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldHandleMissingWeights() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), passJudgment(0.6), passJudgment(0.4));

		// Only provide weight for first judgment - others default to 1.0
		Map<String, Double> weights = Map.of("0", 2.0);

		Judgment result = strategy.aggregate(judgments, weights);

		// (0.8 * 2.0 + 0.6 * 1.0 + 0.4 * 1.0) / (2.0 + 1.0 + 1.0) = (1.6 + 0.6 + 0.4) /
		// 4.0 = 0.65
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.65, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldFallbackToAverageWhenNoWeights() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), passJudgment(0.6));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// No weights → simple average: (0.8 + 0.6) / 2 = 0.7
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.7);
	}

	@Test
	void shouldFallbackToAverageWhenNullWeights() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), passJudgment(0.6));

		Judgment result = strategy.aggregate(judgments, null);

		// Null weights → simple average
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.7);
	}

	@Test
	void shouldHandleBooleanScores() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(booleanPass("Judge 1"), // 1.0
				booleanFail("Judge 2") // 0.0
		);

		Map<String, Double> weights = Map.of("0", 0.7, "1", 0.3);

		Judgment result = strategy.aggregate(judgments, weights);

		// (1.0 * 0.7 + 0.0 * 0.3) / (0.7 + 0.3) = 0.7 / 1.0 = 0.7
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.7);
	}

	@Test
	void shouldHandleZeroWeight() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), passJudgment(0.2));

		// Second judgment has zero weight - should be ignored
		Map<String, Double> weights = Map.of("0", 1.0, "1", 0.0);

		Judgment result = strategy.aggregate(judgments, weights);

		// (0.8 * 1.0 + 0.2 * 0.0) / (1.0 + 0.0) = 0.8 / 1.0 = 0.8
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.8);
	}

	@Test
	void shouldFailWhenWeightedAverageBelowThreshold() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), // low weight
				failJudgment(0.2) // high weight
		);

		Map<String, Double> weights = Map.of("0", 0.2, "1", 0.8);

		Judgment result = strategy.aggregate(judgments, weights);

		// (0.8 * 0.2 + 0.2 * 0.8) / (0.2 + 0.8) = (0.16 + 0.16) / 1.0 = 0.32 < 0.5
		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.32, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldHandleExactThreshold() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(1.0), failJudgment(0.0));

		// Equal weights → (1.0 + 0.0) / 2 = 0.5
		Map<String, Double> weights = Map.of("0", 1.0, "1", 1.0);

		Judgment result = strategy.aggregate(judgments, weights);

		// 0.5 >= 0.5 → PASS
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.5);
	}

	// ==================== Edge Cases ====================

	@Test
	void emptyJudgmentListShouldThrowException() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		assertThatThrownBy(() -> strategy.aggregate(List.of(), Map.of())).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("empty");
	}

	@Test
	void nullJudgmentListShouldThrowException() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		assertThatThrownBy(() -> strategy.aggregate(null, Map.of())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void singleJudgmentShouldReturnItsScore() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		Judgment result = strategy.aggregate(List.of(passJudgment(0.8)), Map.of("0", 5.0));

		// Single judgment with any weight → same score
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.8);
	}

	@Test
	void allZeroWeightsShouldResultInNaN() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), passJudgment(0.6));

		Map<String, Double> weights = Map.of("0", 0.0, "1", 0.0);

		Judgment result = strategy.aggregate(judgments, weights);

		// All zero weights → 0/0 results in NaN
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isNaN();
	}

	// ==================== Metadata Tests ====================

	@Test
	void shouldReturnCorrectName() {
		WeightedAverageStrategy strategy = new WeightedAverageStrategy();

		assertThat(strategy.getName()).isEqualTo("WeightedAverage");
	}

}
