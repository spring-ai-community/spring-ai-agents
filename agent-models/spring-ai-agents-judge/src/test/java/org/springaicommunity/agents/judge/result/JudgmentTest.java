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

package org.springaicommunity.agents.judge.result;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springaicommunity.agents.judge.score.NumericalScore;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Judgment}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class JudgmentTest {

	@Test
	void shouldCreateJudgmentWithBuilder() {
		BooleanScore score = new BooleanScore(true);
		Check check = Check.pass("Test passed");

		Judgment judgment = Judgment.builder()
			.score(score)
			.status(JudgmentStatus.PASS)
			.reasoning("All tests passed")
			.check(check)
			.metadata("elapsed", Duration.ofMillis(100))
			.build();

		assertThat(judgment.score()).isEqualTo(score);
		assertThat(judgment.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(judgment.reasoning()).isEqualTo("All tests passed");
		assertThat(judgment.checks()).containsExactly(check);
		assertThat(judgment.metadata()).containsEntry("elapsed", Duration.ofMillis(100));
	}

	@Test
	void shouldCreatePassJudgmentWithConvenience() {
		Judgment judgment = Judgment.pass("Simple pass");

		assertThat(judgment.status()).isEqualTo(JudgmentStatus.PASS);
		assertThat(judgment.reasoning()).isEqualTo("Simple pass");
		assertThat(judgment.score()).isInstanceOf(BooleanScore.class);
		assertThat(((BooleanScore) judgment.score()).value()).isTrue();
		assertThat(judgment.pass()).isTrue();
	}

	@Test
	void shouldCreateFailJudgmentWithConvenience() {
		Judgment judgment = Judgment.fail("Simple fail");

		assertThat(judgment.status()).isEqualTo(JudgmentStatus.FAIL);
		assertThat(judgment.reasoning()).isEqualTo("Simple fail");
		assertThat(judgment.score()).isInstanceOf(BooleanScore.class);
		assertThat(((BooleanScore) judgment.score()).value()).isFalse();
		assertThat(judgment.pass()).isFalse();
	}

	@Test
	void shouldCreateAbstainJudgmentWithConvenience() {
		Judgment judgment = Judgment.abstain("Cannot evaluate");

		assertThat(judgment.status()).isEqualTo(JudgmentStatus.ABSTAIN);
		assertThat(judgment.reasoning()).isEqualTo("Cannot evaluate");
		assertThat(judgment.score()).isInstanceOf(BooleanScore.class);
		assertThat(judgment.pass()).isFalse();
	}

	@Test
	void shouldCreateErrorJudgmentWithThrowable() {
		RuntimeException error = new RuntimeException("Test error");
		Judgment judgment = Judgment.error("Evaluation failed", error);

		assertThat(judgment.status()).isEqualTo(JudgmentStatus.ERROR);
		assertThat(judgment.reasoning()).isEqualTo("Evaluation failed");
		assertThat(judgment.error()).isEqualTo(error);
		assertThat(judgment.pass()).isFalse();
	}

	@Test
	void shouldCreateErrorJudgmentWithoutThrowable() {
		Judgment judgment = Judgment.error("Evaluation failed", null);

		assertThat(judgment.status()).isEqualTo(JudgmentStatus.ERROR);
		assertThat(judgment.reasoning()).isEqualTo("Evaluation failed");
		assertThat(judgment.error()).isNull();
	}

	@Test
	void shouldSupportPassHelper() {
		Judgment pass = Judgment.pass("Passed");
		Judgment fail = Judgment.fail("Failed");

		assertThat(pass.pass()).isTrue();
		assertThat(fail.pass()).isFalse();
	}

	@Test
	void shouldRetrieveElapsedFromMetadata() {
		Duration elapsed = Duration.ofMillis(500);

		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("Test")
			.metadata("elapsed", elapsed)
			.build();

		assertThat(judgment.elapsed()).isEqualTo(elapsed);
	}

	@Test
	void shouldReturnNullElapsedWhenNotPresent() {
		Judgment judgment = Judgment.pass("Test");

		assertThat(judgment.elapsed()).isNull();
	}

	@Test
	void shouldRetrieveErrorFromMetadata() {
		RuntimeException error = new RuntimeException("Test error");

		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(false))
			.status(JudgmentStatus.ERROR)
			.reasoning("Error occurred")
			.metadata("error", error)
			.build();

		assertThat(judgment.error()).isEqualTo(error);
	}

	@Test
	void shouldReturnNullErrorWhenNotPresent() {
		Judgment judgment = Judgment.fail("Test");

		assertThat(judgment.error()).isNull();
	}

	// ==================== Builder Tests ====================

	@Test
	void builderShouldSupportMultipleChecks() {
		Check check1 = Check.pass("Check 1");
		Check check2 = Check.fail("Check 2", "Failed");

		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(false))
			.status(JudgmentStatus.FAIL)
			.reasoning("Some checks failed")
			.check(check1)
			.check(check2)
			.build();

		assertThat(judgment.checks()).containsExactly(check1, check2);
	}

	@Test
	void builderShouldSupportChecksList() {
		List<Check> checks = List.of(Check.pass("C1"), Check.pass("C2"), Check.fail("C3", "Failed"));

		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(false))
			.status(JudgmentStatus.FAIL)
			.reasoning("One check failed")
			.checks(checks)
			.build();

		assertThat(judgment.checks()).hasSize(3);
		assertThat(judgment.checks()).containsExactlyElementsOf(checks);
	}

	@Test
	void builderShouldSupportMetadataMap() {
		Map<String, Object> metadata = Map.of("elapsed", Duration.ofMillis(100), "attempt", 1);

		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("Test")
			.metadata(metadata)
			.build();

		assertThat(judgment.metadata()).containsEntry("elapsed", Duration.ofMillis(100));
		assertThat(judgment.metadata()).containsEntry("attempt", 1);
	}

	@Test
	void builderShouldSupportMetadataKeyValue() {
		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("Test")
			.metadata("key1", "value1")
			.metadata("key2", 42)
			.build();

		assertThat(judgment.metadata()).containsEntry("key1", "value1");
		assertThat(judgment.metadata()).containsEntry("key2", 42);
	}

	@Test
	void builderShouldDefaultReasoningToEmpty() {
		Judgment judgment = Judgment.builder().score(new BooleanScore(true)).status(JudgmentStatus.PASS).build();

		assertThat(judgment.reasoning()).isEmpty();
	}

	@Test
	void builderShouldCreateDefensiveCopiesOfChecks() {
		List<Check> originalChecks = new java.util.ArrayList<>();
		originalChecks.add(Check.pass("C1"));

		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("Test")
			.checks(originalChecks)
			.build();

		// Modify original - should not affect judgment
		originalChecks.add(Check.pass("C2"));

		assertThat(judgment.checks()).hasSize(1);
	}

	@Test
	void builderShouldCreateDefensiveCopiesOfMetadata() {
		Map<String, Object> originalMetadata = new java.util.HashMap<>();
		originalMetadata.put("key1", "value1");

		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("Test")
			.metadata(originalMetadata)
			.build();

		// Modify original - should not affect judgment
		originalMetadata.put("key2", "value2");

		assertThat(judgment.metadata()).hasSize(1);
		assertThat(judgment.metadata()).containsOnlyKeys("key1");
	}

	// ==================== Record Tests ====================

	@Test
	void recordShouldProvideImmutableChecks() {
		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("Test")
			.check(Check.pass("C1"))
			.build();

		assertThat(judgment.checks()).isUnmodifiable();
	}

	@Test
	void recordShouldProvideImmutableMetadata() {
		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("Test")
			.metadata("key", "value")
			.build();

		assertThat(judgment.metadata()).isUnmodifiable();
	}

	@Test
	void recordShouldProvideEquality() {
		BooleanScore score = new BooleanScore(true);
		Judgment j1 = Judgment.builder().score(score).status(JudgmentStatus.PASS).reasoning("Test").build();

		Judgment j2 = Judgment.builder().score(score).status(JudgmentStatus.PASS).reasoning("Test").build();

		assertThat(j1).isEqualTo(j2);
		assertThat(j1.hashCode()).isEqualTo(j2.hashCode());
	}

	@Test
	void recordShouldProvideToString() {
		Judgment judgment = Judgment.pass("Test reasoning");

		String toString = judgment.toString();

		assertThat(toString).contains("Judgment");
		assertThat(toString).contains("PASS");
		assertThat(toString).contains("Test reasoning");
	}

	// ==================== Score Integration Tests ====================

	@Test
	void shouldSupportNumericalScore() {
		NumericalScore score = new NumericalScore(0.85, 0.0, 1.0);

		Judgment judgment = Judgment.builder()
			.score(score)
			.status(JudgmentStatus.PASS)
			.reasoning("Score above threshold")
			.build();

		assertThat(judgment.score()).isEqualTo(score);
		assertThat(((NumericalScore) judgment.score()).normalized()).isCloseTo(0.85,
				org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void shouldSupportMultipleMetadataEntries() {
		Judgment judgment = Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("Test")
			.metadata("elapsed", Duration.ofMillis(100))
			.metadata("attempt", 1)
			.metadata("retries", 0)
			.build();

		assertThat(judgment.metadata()).hasSize(3);
		assertThat(judgment.elapsed()).isEqualTo(Duration.ofMillis(100));
	}

}
