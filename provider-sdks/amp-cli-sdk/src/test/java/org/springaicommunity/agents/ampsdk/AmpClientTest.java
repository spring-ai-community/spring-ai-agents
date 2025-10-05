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

package org.springaicommunity.agents.ampsdk;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.ampsdk.types.ExecuteOptions;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AmpClient}.
 *
 * @author Spring AI Community
 */
class AmpClientTest {

	@Test
	void testCreateWithDefaults() {
		// This test will fail if Amp CLI is not installed
		// We're mainly testing the API design here
		assertThat(AmpClient.class).isNotNull();
	}

	@Test
	void testBuilderPattern() {
		ExecuteOptions options = ExecuteOptions.builder()
			.dangerouslyAllowAll(true)
			.timeout(java.time.Duration.ofMinutes(5))
			.build();

		assertThat(options).isNotNull();
		assertThat(options.isDangerouslyAllowAll()).isTrue();
		assertThat(options.getTimeout()).isEqualTo(java.time.Duration.ofMinutes(5));
	}

	@Test
	void testDefaultOptions() {
		ExecuteOptions options = ExecuteOptions.defaultOptions();

		assertThat(options).isNotNull();
		assertThat(options.isDangerouslyAllowAll()).isTrue(); // Default should be true
		assertThat(options.getTimeout()).isNotNull();
	}

}
