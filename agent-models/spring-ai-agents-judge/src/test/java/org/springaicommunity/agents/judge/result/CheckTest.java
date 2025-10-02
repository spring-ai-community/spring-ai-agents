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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Check}.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class CheckTest {

	@Test
	void shouldCreatePassCheck() {
		Check check = Check.pass("Test passed");

		assertThat(check.name()).isEqualTo("Test passed");
		assertThat(check.passed()).isTrue();
		assertThat(check.message()).isEmpty();
	}

	@Test
	void shouldCreatePassCheckWithMessage() {
		Check check = Check.pass("Test passed", "All assertions succeeded");

		assertThat(check.name()).isEqualTo("Test passed");
		assertThat(check.passed()).isTrue();
		assertThat(check.message()).isEqualTo("All assertions succeeded");
	}

	@Test
	void shouldCreateFailCheck() {
		Check check = Check.fail("Test failed", "Expected 5 but was 3");

		assertThat(check.name()).isEqualTo("Test failed");
		assertThat(check.passed()).isFalse();
		assertThat(check.message()).isEqualTo("Expected 5 but was 3");
	}

	@Test
	void shouldCreateCheckWithConstructor() {
		Check check = new Check("Custom check", true, "Custom message");

		assertThat(check.name()).isEqualTo("Custom check");
		assertThat(check.passed()).isTrue();
		assertThat(check.message()).isEqualTo("Custom message");
	}

	// ==================== Record Tests ====================

	@Test
	void recordShouldProvideEquality() {
		Check c1 = Check.pass("Test", "Message");
		Check c2 = Check.pass("Test", "Message");

		assertThat(c1).isEqualTo(c2);
		assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
	}

	@Test
	void recordShouldProvideToString() {
		Check check = Check.fail("Build", "Compilation failed");

		String toString = check.toString();

		assertThat(toString).contains("Check");
		assertThat(toString).contains("Build");
		assertThat(toString).contains("Compilation failed");
	}

	@Test
	void recordShouldDistinguishPassFromFail() {
		Check pass = Check.pass("Test");
		Check fail = Check.fail("Test", "Failed");

		assertThat(pass).isNotEqualTo(fail);
	}

	// ==================== Use Case Tests ====================

	@Test
	void shouldSupportMultipleChecksInJudgment() {
		Check compilationCheck = Check.pass("Compilation");
		Check testCheck = Check.pass("Tests ran");
		Check coverageCheck = Check.fail("Coverage", "Only 70%, expected 80%");

		assertThat(compilationCheck.passed()).isTrue();
		assertThat(testCheck.passed()).isTrue();
		assertThat(coverageCheck.passed()).isFalse();
		assertThat(coverageCheck.message()).contains("70%");
	}

	@Test
	void shouldHandleEmptyMessage() {
		Check check = new Check("Test", true, "");

		assertThat(check.message()).isEmpty();
	}

}
