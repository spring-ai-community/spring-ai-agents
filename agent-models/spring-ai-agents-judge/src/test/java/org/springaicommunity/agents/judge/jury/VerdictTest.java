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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springaicommunity.agents.judge.JudgeTestFixtures.*;

/**
 * Tests for {@link Verdict}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class VerdictTest {

	@Test
	void shouldBuildVerdictWithBuilder() {
		Judgment aggregated = booleanPass("Majority passed");
		List<Judgment> individual = List.of(booleanPass("Judge 1"), booleanFail("Judge 2"), booleanPass("Judge 3"));
		Map<String, Judgment> byName = Map.of("Judge1", individual.get(0), "Judge2", individual.get(1), "Judge3",
				individual.get(2));
		Map<String, Double> weights = Map.of("0", 0.3, "1", 0.5, "2", 0.2);

		Verdict verdict = Verdict.builder()
			.aggregated(aggregated)
			.individual(individual)
			.individualByName(byName)
			.weights(weights)
			.build();

		assertThat(verdict.aggregated()).isEqualTo(aggregated);
		assertThat(verdict.individual()).hasSize(3);
		assertThat(verdict.individualByName()).hasSize(3);
		assertThat(verdict.weights()).hasSize(3);
		assertThat(verdict.subVerdicts()).isEmpty();
	}

	@Test
	void shouldSupportSubVerdictsForMetaJury() {
		Verdict subVerdict1 = unanimousPass(2);
		Verdict subVerdict2 = split(1, 1);

		Judgment metaAggregated = booleanPass("Meta-jury passed");

		Verdict metaVerdict = Verdict.builder()
			.aggregated(metaAggregated)
			.individual(List.of(subVerdict1.aggregated(), subVerdict2.aggregated()))
			.subVerdicts(List.of(subVerdict1, subVerdict2))
			.build();

		assertThat(metaVerdict.aggregated()).isEqualTo(metaAggregated);
		assertThat(metaVerdict.subVerdicts()).hasSize(2);
		assertThat(metaVerdict.subVerdicts().get(0)).isEqualTo(subVerdict1);
		assertThat(metaVerdict.subVerdicts().get(1)).isEqualTo(subVerdict2);
	}

	@Test
	void shouldProvideDefensiveCopiesForImmutability() {
		List<Judgment> originalIndividual = new java.util.ArrayList<>();
		originalIndividual.add(booleanPass("Judge 1"));
		originalIndividual.add(booleanFail("Judge 2"));

		Map<String, Judgment> originalByName = new java.util.HashMap<>();
		originalByName.put("Judge1", booleanPass("Judge 1"));

		Verdict verdict = Verdict.builder()
			.aggregated(booleanPass("Aggregated"))
			.individual(originalIndividual)
			.individualByName(originalByName)
			.build();

		// Modify originals - should not affect verdict
		originalIndividual.add(booleanPass("Judge 3"));
		originalByName.put("Judge2", booleanFail("Judge 2"));

		assertThat(verdict.individual()).hasSize(2);
		assertThat(verdict.individualByName()).hasSize(1);
	}

	@Test
	void shouldReturnUnmodifiableCollections() {
		Verdict verdict = Verdict.builder()
			.aggregated(booleanPass("Aggregated"))
			.individual(List.of(booleanPass("Judge 1")))
			.individualByName(Map.of("Judge1", booleanPass("Judge 1")))
			.build();

		// All collections should be immutable
		assertThat(verdict.individual()).isInstanceOf(List.class);
		assertThat(verdict.individualByName()).isInstanceOf(Map.class);

		// Attempting to modify should throw UnsupportedOperationException
		assertThat(verdict.individual()).isUnmodifiable();
		assertThat(verdict.individualByName()).isUnmodifiable();
	}

	@Test
	void shouldHandleNullFieldsWithDefaults() {
		Verdict verdict = Verdict.builder().aggregated(booleanPass("Only aggregated")).build();

		assertThat(verdict.aggregated()).isNotNull();
		assertThat(verdict.individual()).isEmpty();
		assertThat(verdict.individualByName()).isEmpty();
		assertThat(verdict.weights()).isEmpty();
		assertThat(verdict.subVerdicts()).isEmpty();
	}

	@Test
	void shouldHandleEmptyCollections() {
		Verdict verdict = Verdict.builder()
			.aggregated(booleanPass("Aggregated"))
			.individual(List.of())
			.individualByName(Map.of())
			.weights(Map.of())
			.subVerdicts(List.of())
			.build();

		assertThat(verdict.individual()).isEmpty();
		assertThat(verdict.individualByName()).isEmpty();
		assertThat(verdict.weights()).isEmpty();
		assertThat(verdict.subVerdicts()).isEmpty();
	}

	@Test
	void shouldPreserveJudgeIdentity() {
		Judgment judge1 = booleanPass("File exists");
		Judgment judge2 = booleanFail("Correctness failed");
		Judgment judge3 = booleanPass("Build succeeded");

		Map<String, Judgment> byName = Map.of("FileExistsJudge", judge1, "CorrectnessJudge", judge2,
				"BuildSuccessJudge", judge3);

		Verdict verdict = Verdict.builder()
			.aggregated(booleanPass("Majority passed"))
			.individual(List.of(judge1, judge2, judge3))
			.individualByName(byName)
			.build();

		assertThat(verdict.individualByName().get("FileExistsJudge")).isEqualTo(judge1);
		assertThat(verdict.individualByName().get("CorrectnessJudge")).isEqualTo(judge2);
		assertThat(verdict.individualByName().get("BuildSuccessJudge")).isEqualTo(judge3);
	}

	@Test
	void shouldSupportWeightsInVerdict() {
		Map<String, Double> weights = Map.of("0", 0.5, "1", 0.3, "2", 0.2);

		Verdict verdict = Verdict.builder()
			.aggregated(booleanPass("Weighted result"))
			.individual(List.of(booleanPass("J1"), booleanPass("J2"), booleanPass("J3")))
			.weights(weights)
			.build();

		assertThat(verdict.weights()).containsEntry("0", 0.5);
		assertThat(verdict.weights()).containsEntry("1", 0.3);
		assertThat(verdict.weights()).containsEntry("2", 0.2);
	}

	@Test
	void shouldAllowNullAggregated() {
		// Builder allows null aggregated (though not typical)
		Verdict verdict = Verdict.builder().individual(List.of(booleanPass("Judge 1"))).aggregated(null).build();

		assertThat(verdict.aggregated()).isNull();
		assertThat(verdict.individual()).hasSize(1);
	}

	@Test
	void recordShouldProvideEquality() {
		Judgment agg = booleanPass("Aggregated");
		List<Judgment> ind = List.of(booleanPass("J1"));

		Verdict verdict1 = Verdict.builder().aggregated(agg).individual(ind).build();

		Verdict verdict2 = Verdict.builder().aggregated(agg).individual(ind).build();

		assertThat(verdict1).isEqualTo(verdict2);
		assertThat(verdict1.hashCode()).isEqualTo(verdict2.hashCode());
	}

	@Test
	void recordShouldProvideToString() {
		Verdict verdict = Verdict.builder()
			.aggregated(booleanPass("Aggregated"))
			.individual(List.of(booleanPass("J1")))
			.build();

		String toString = verdict.toString();

		assertThat(toString).contains("Verdict");
		assertThat(toString).contains("aggregated");
		assertThat(toString).contains("individual");
	}

}
