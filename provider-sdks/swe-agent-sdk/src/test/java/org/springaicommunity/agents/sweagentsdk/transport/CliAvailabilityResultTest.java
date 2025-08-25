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

package org.springaicommunity.agents.sweagentsdk.transport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CliAvailabilityResult.
 */
class CliAvailabilityResultTest {

	@Test
	void testAvailableResult() {
		// Given
		String version = "1.4.2";

		// When
		CliAvailabilityResult result = CliAvailabilityResult.available(version);

		// Then
		assertThat(result.isAvailable()).isTrue();
		assertThat(result.getVersion()).isPresent().hasValue(version);
		assertThat(result.getReason()).isEmpty();
		assertThat(result.getException()).isEmpty();
	}

	@Test
	void testUnavailableResult() {
		// Given
		String reason = "Command not found";

		// When
		CliAvailabilityResult result = CliAvailabilityResult.unavailable(reason);

		// Then
		assertThat(result.isAvailable()).isFalse();
		assertThat(result.getVersion()).isEmpty();
		assertThat(result.getReason()).isPresent().hasValue(reason);
		assertThat(result.getException()).isEmpty();
	}

	@Test
	void testUnavailableResultWithException() {
		// Given
		String reason = "Process failed";
		Exception exception = new RuntimeException("Test exception");

		// When
		CliAvailabilityResult result = CliAvailabilityResult.unavailable(reason, exception);

		// Then
		assertThat(result.isAvailable()).isFalse();
		assertThat(result.getVersion()).isEmpty();
		assertThat(result.getReason()).isPresent().hasValue(reason);
		assertThat(result.getException()).isPresent().hasValue(exception);
	}

	@Test
	void testToStringForAvailable() {
		// Given
		String version = "1.4.2";
		CliAvailabilityResult result = CliAvailabilityResult.available(version);

		// When
		String toString = result.toString();

		// Then
		assertThat(toString).contains("available=true");
		assertThat(toString).contains("version='1.4.2'");
	}

	@Test
	void testToStringForUnavailable() {
		// Given
		String reason = "Command not found";
		CliAvailabilityResult result = CliAvailabilityResult.unavailable(reason);

		// When
		String toString = result.toString();

		// Then
		assertThat(toString).contains("available=false");
		assertThat(toString).contains("reason='Command not found'");
	}

	@Test
	void testEquals() {
		// Given
		String version = "1.4.2";
		CliAvailabilityResult result1 = CliAvailabilityResult.available(version);
		CliAvailabilityResult result2 = CliAvailabilityResult.available(version);
		CliAvailabilityResult result3 = CliAvailabilityResult.available("1.5.0");

		// Then
		assertThat(result1).isEqualTo(result2);
		assertThat(result1).isNotEqualTo(result3);
		assertThat(result1).isNotEqualTo(null);
		assertThat(result1).isNotEqualTo("not a result");
	}

	@Test
	void testHashCode() {
		// Given
		String version = "1.4.2";
		CliAvailabilityResult result1 = CliAvailabilityResult.available(version);
		CliAvailabilityResult result2 = CliAvailabilityResult.available(version);

		// Then
		assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
	}

	@Test
	void testUnavailableEquals() {
		// Given
		String reason = "Command not found";
		CliAvailabilityResult result1 = CliAvailabilityResult.unavailable(reason);
		CliAvailabilityResult result2 = CliAvailabilityResult.unavailable(reason);
		CliAvailabilityResult result3 = CliAvailabilityResult.unavailable("Different reason");

		// Then
		assertThat(result1).isEqualTo(result2);
		assertThat(result1).isNotEqualTo(result3);
	}

}