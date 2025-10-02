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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreTest {

	@Test
	void booleanScoreWorks() {
		BooleanScore pass = new BooleanScore(true);
		assertThat(pass.value()).isTrue();
		assertThat(pass.type()).isEqualTo(ScoreType.BOOLEAN);

		BooleanScore fail = new BooleanScore(false);
		assertThat(fail.value()).isFalse();
		assertThat(fail.type()).isEqualTo(ScoreType.BOOLEAN);
	}

	@Test
	void numericalScoreValidatesRange() {
		NumericalScore score = new NumericalScore(7.5, 0, 10);
		assertThat(score.value()).isEqualTo(7.5);
		assertThat(score.type()).isEqualTo(ScoreType.NUMERICAL);

		assertThatThrownBy(() -> new NumericalScore(11, 0, 10)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("must be between");
	}

	@Test
	void numericalScoreNormalizes() {
		NumericalScore score = new NumericalScore(7.5, 0, 10);
		assertThat(score.normalized()).isEqualTo(0.75);

		NumericalScore zeroToOne = NumericalScore.normalized(0.85);
		assertThat(zeroToOne.value()).isEqualTo(0.85);
		assertThat(zeroToOne.normalized()).isEqualTo(0.85);

		NumericalScore outOf10 = NumericalScore.outOfTen(8.5);
		assertThat(outOf10.value()).isEqualTo(8.5);
		assertThat(outOf10.normalized()).isEqualTo(0.85);
	}

	@Test
	void categoricalScoreValidatesAllowedValues() {
		CategoricalScore score = new CategoricalScore("good", List.of("poor", "fair", "good", "excellent"));
		assertThat(score.value()).isEqualTo("good");
		assertThat(score.type()).isEqualTo(ScoreType.CATEGORICAL);

		assertThatThrownBy(() -> new CategoricalScore("bad", List.of("poor", "fair", "good", "excellent")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("must be one of");
	}

}
