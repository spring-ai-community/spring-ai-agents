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
 * Tests for {@link MedianVotingStrategy}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class MedianVotingStrategyTest {

	@Test
	void shouldCalculateMedianForOddCount() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.3), passJudgment(0.7), // median
				passJudgment(0.9));

		Judgment result = strategy.aggregate(judgments, Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(result.score()).isInstanceOf(NumericalScore.class);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.7);
	}

	@Test
	void shouldCalculateMedianForEvenCount() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.3), passJudgment(0.5), // middle
																					// values
				passJudgment(0.7), // middle values
				passJudgment(0.9));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Median of even count: (0.5 + 0.7) / 2 = 0.6
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.6);
	}

	@Test
	void shouldSortScoresBeforeCalculatingMedian() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		// Unsorted input
		List<Judgment> judgments = List.of(passJudgment(0.9), passJudgment(0.3), passJudgment(0.7));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// After sorting: [0.3, 0.7, 0.9] → median = 0.7
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.7);
	}

	@Test
	void shouldBeRobustToOutliers() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		// Extreme outliers don't affect median
		List<Judgment> judgments = List.of(passJudgment(0.0), // outlier
				passJudgment(0.5), passJudgment(0.6), // median around here
				passJudgment(1.0) // outlier
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Sorted: [0.0, 0.5, 0.6, 1.0] → median = (0.5 + 0.6) / 2 = 0.55
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.55, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldHandleBooleanScores() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(booleanFail("Judge 1"), // 0.0
				booleanPass("Judge 2"), // 1.0
				booleanPass("Judge 3") // 1.0
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Sorted: [0.0, 1.0, 1.0] → median = 1.0
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(1.0);
	}

	@Test
	void shouldHandleMixedBooleanAndNumerical() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(booleanFail("Judge 1"), // 0.0
				passJudgment(0.6), booleanPass("Judge 3") // 1.0
		);

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Sorted: [0.0, 0.6, 1.0] → median = 0.6
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.6);
	}

	@Test
	void shouldFailWhenMedianBelowThreshold() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(failJudgment(0.1), failJudgment(0.3), failJudgment(0.4));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Median = 0.3 < 0.5 → FAIL
		assertThat(result.status()).isEqualTo(JudgmentStatus.FAIL);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.3);
	}

	@Test
	void shouldHandleExactThreshold() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(failJudgment(0.0), passJudgment(0.5), passJudgment(1.0));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Median = 0.5 >= 0.5 → PASS
		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.5);
	}

	@Test
	void shouldIgnoreWeightsParameter() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.3), passJudgment(0.7), passJudgment(0.9));

		// Weights are ignored in MedianVotingStrategy
		Map<String, Double> weights = Map.of("0", 10.0, "1", 1.0, "2", 1.0);

		Judgment result = strategy.aggregate(judgments, weights);

		// Median calculation ignores weights → 0.7
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.7);
	}

	// ==================== Edge Cases ====================

	@Test
	void emptyJudgmentListShouldThrowException() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		assertThatThrownBy(() -> strategy.aggregate(List.of(), Map.of())).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("empty");
	}

	@Test
	void nullJudgmentListShouldThrowException() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		assertThatThrownBy(() -> strategy.aggregate(null, Map.of())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void singleJudgmentShouldReturnItsScore() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		Judgment result = strategy.aggregate(List.of(passJudgment(0.8)), Map.of());

		assertThat(result.status()).isEqualTo(JudgmentStatus.PASS);
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.8);
	}

	@Test
	void twoJudgmentsShouldAverageThemAsMedian() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.4), passJudgment(0.8));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Even count: (0.4 + 0.8) / 2 = 0.6
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isCloseTo(0.6, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void allSameScoresShouldReturnThatScore() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.7), passJudgment(0.7), passJudgment(0.7));

		Judgment result = strategy.aggregate(judgments, Map.of());

		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.7);
	}

	@Test
	void shouldHandleNullScoresAsZero() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		List<Judgment> judgments = List.of(passJudgment(0.8), Judgment.abstain("Cannot evaluate"), // null
																									// score
																									// →
																									// 0.0
				passJudgment(0.6));

		Judgment result = strategy.aggregate(judgments, Map.of());

		// Sorted: [0.0, 0.6, 0.8] → median = 0.6
		NumericalScore score = (NumericalScore) result.score();
		assertThat(score.normalized()).isEqualTo(0.6);
	}

	// ==================== Metadata Tests ====================

	@Test
	void shouldReturnCorrectName() {
		MedianVotingStrategy strategy = new MedianVotingStrategy();

		assertThat(strategy.getName()).isEqualTo("MedianVoting");
	}

}
