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

package org.springaicommunity.agents.judge.score;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link Scores} utility class.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class ScoresTest {

	@Test
	void shouldConvertNullScoreToZero() {
		double result = Scores.toNormalized(null, Map.of());

		assertThat(result).isEqualTo(0.0);
	}

	@Test
	void shouldConvertBooleanTrueToOne() {
		BooleanScore score = new BooleanScore(true);

		double result = Scores.toNormalized(score, Map.of());

		assertThat(result).isEqualTo(1.0);
	}

	@Test
	void shouldConvertBooleanFalseToZero() {
		BooleanScore score = new BooleanScore(false);

		double result = Scores.toNormalized(score, Map.of());

		assertThat(result).isEqualTo(0.0);
	}

	@Test
	void shouldConvertNumericalScoreToNormalized() {
		NumericalScore score = new NumericalScore(75.0, 0.0, 100.0);

		double result = Scores.toNormalized(score, Map.of());

		assertThat(result).isCloseTo(0.75, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldConvertCategoricalScoreWithMapping() {
		CategoricalScore score = new CategoricalScore("excellent", java.util.List.of("poor", "good", "excellent"));
		Map<String, Double> categoryMap = Map.of("poor", 0.2, "good", 0.6, "excellent", 1.0);

		double result = Scores.toNormalized(score, categoryMap);

		assertThat(result).isEqualTo(1.0);
	}

	@Test
	void shouldDefaultToZeroForUnmappedCategory() {
		CategoricalScore score = new CategoricalScore("unknown", java.util.List.of("unknown", "good", "excellent"));
		Map<String, Double> categoryMap = Map.of("good", 0.6, "excellent", 1.0);

		double result = Scores.toNormalized(score, categoryMap);

		assertThat(result).isEqualTo(0.0);
	}

	@Test
	void shouldHandleCategoricalScoreWithEmptyMap() {
		CategoricalScore score = new CategoricalScore("any", java.util.List.of("any", "other"));

		double result = Scores.toNormalized(score, Map.of());

		assertThat(result).isEqualTo(0.0);
	}

	// ==================== Edge Case Tests ====================

	@Test
	void shouldHandleNumericalScoreAtBoundaries() {
		NumericalScore minScore = new NumericalScore(0.0, 0.0, 100.0);
		NumericalScore maxScore = new NumericalScore(100.0, 0.0, 100.0);

		assertThat(Scores.toNormalized(minScore, Map.of())).isEqualTo(0.0);
		assertThat(Scores.toNormalized(maxScore, Map.of())).isEqualTo(1.0);
	}

	@Test
	void shouldHandleNumericalScoreWithNonStandardRange() {
		// Score of 50 in range [-100, 100] → normalized to 0.75
		NumericalScore score = new NumericalScore(50.0, -100.0, 100.0);

		double result = Scores.toNormalized(score, Map.of());

		assertThat(result).isCloseTo(0.75, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldHandleCategoricalWithPartialMapping() {
		CategoricalScore score = new CategoricalScore("medium", java.util.List.of("low", "medium", "high"));
		Map<String, Double> categoryMap = Map.of("low", 0.0, "medium", 0.5, "high", 1.0);

		double result = Scores.toNormalized(score, categoryMap);

		assertThat(result).isEqualTo(0.5);
	}

	// ==================== Integration Tests ====================

	@Test
	void shouldSupportHeterogeneousScoreNormalization() {
		// Simulate voting strategy aggregating different score types
		BooleanScore boolScore = new BooleanScore(true);
		NumericalScore numScore = new NumericalScore(80.0, 0.0, 100.0);
		CategoricalScore catScore = new CategoricalScore("good", java.util.List.of("poor", "good", "excellent"));

		Map<String, Double> categoryMap = Map.of("poor", 0.2, "good", 0.6, "excellent", 1.0);

		double bool = Scores.toNormalized(boolScore, categoryMap);
		double num = Scores.toNormalized(numScore, categoryMap);
		double cat = Scores.toNormalized(catScore, categoryMap);

		// All normalized to [0.0, 1.0] range
		assertThat(bool).isEqualTo(1.0);
		assertThat(num).isCloseTo(0.8, org.assertj.core.data.Offset.offset(0.01));
		assertThat(cat).isEqualTo(0.6);

		// Can now compute average
		double average = (bool + num + cat) / 3.0;
		assertThat(average).isCloseTo(0.8, org.assertj.core.data.Offset.offset(0.01));
	}

}
