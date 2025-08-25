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

package org.springaicommunity.agents.geminisdk.transport;

import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CLIOptions configuration and validation.
 */
class CLIOptionsTest {

	@Test
	void testDefaultOptions() {
		CLIOptions options = CLIOptions.defaultOptions();

		assertThat(options.model()).isNull();
		assertThat(options.yoloMode()).isTrue();
		assertThat(options.allFiles()).isFalse();
		assertThat(options.timeout()).isEqualTo(Duration.ofMinutes(2));
	}

	@Test
	void testBuilderDefaults() {
		CLIOptions options = CLIOptions.builder().build();

		assertThat(options.model()).isNull();
		assertThat(options.yoloMode()).isTrue();
		assertThat(options.allFiles()).isFalse();
		assertThat(options.timeout()).isEqualTo(Duration.ofMinutes(2));
	}

	@Test
	void testBuilderWithValues() {
		CLIOptions options = CLIOptions.builder()
			.model("gemini-2.5-pro")
			.yoloMode(false)
			.allFiles(true)
			.timeout(Duration.ofMinutes(5))
			.build();

		assertThat(options.model()).isEqualTo("gemini-2.5-pro");
		assertThat(options.yoloMode()).isFalse();
		assertThat(options.allFiles()).isTrue();
		assertThat(options.timeout()).isEqualTo(Duration.ofMinutes(5));
	}

	@Test
	void testTimeoutConvenienceMethods() {
		CLIOptions options1 = CLIOptions.builder().timeoutSeconds(30).build();
		assertThat(options1.timeout()).isEqualTo(Duration.ofSeconds(30));

		CLIOptions options2 = CLIOptions.builder().timeoutMinutes(3).build();
		assertThat(options2.timeout()).isEqualTo(Duration.ofMinutes(3));
	}

	@Test
	void testValidModelNames() {
		// Valid model names should not throw exceptions
		assertThatNoException().isThrownBy(() -> CLIOptions.builder().model("gemini-2.5-pro").build());
		assertThatNoException().isThrownBy(() -> CLIOptions.builder().model("gemini-2.0-flash-exp").build());
		assertThatNoException().isThrownBy(() -> CLIOptions.builder().model("gemini-pro").build());
		assertThatNoException().isThrownBy(() -> CLIOptions.builder().model("gemini-flash").build());
		assertThatNoException().isThrownBy(() -> CLIOptions.builder().model("models/gemini-2.5-pro").build());
	}

	@Test
	void testInvalidModelNames() {
		assertThatThrownBy(() -> CLIOptions.builder().model("invalid-model").build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid model name");

		assertThatThrownBy(() -> CLIOptions.builder().model("gpt-4").build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid model name");

		assertThatThrownBy(() -> CLIOptions.builder().model("").build()).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testTimeoutValidation() {
		// Negative timeout should fail
		assertThatThrownBy(() -> CLIOptions.builder().timeout(Duration.ofMinutes(-1)).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("cannot be negative");

		// Very long timeout should fail
		assertThatThrownBy(() -> CLIOptions.builder().timeout(Duration.ofHours(2)).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("cannot exceed 30 minutes");
	}

	@Test
	void testPresetConfigurations() {
		CLIOptions fastResponse = CLIOptions.fastResponse();
		assertThat(fastResponse.model()).isEqualTo("gemini-2.0-flash-exp");
		assertThat(fastResponse.timeout()).isEqualTo(Duration.ofSeconds(30));
		assertThat(fastResponse.yoloMode()).isTrue();

		CLIOptions highQuality = CLIOptions.highQuality();
		assertThat(highQuality.model()).isEqualTo("gemini-2.5-pro");
		assertThat(highQuality.timeout()).isEqualTo(Duration.ofMinutes(5));
		assertThat(highQuality.yoloMode()).isTrue();

		CLIOptions development = CLIOptions.development();
		assertThat(development.model()).isEqualTo("gemini-2.0-flash-exp");
		assertThat(development.timeout()).isEqualTo(Duration.ofMinutes(1));
		assertThat(development.allFiles()).isFalse();
		assertThat(development.yoloMode()).isTrue();
	}

	@Test
	void testNullValues() {
		// Null model should be acceptable
		assertThatNoException().isThrownBy(() -> CLIOptions.builder().model(null).build());

		// Null timeout should default to 2 minutes
		CLIOptions options = CLIOptions.builder().timeout(null).build();
		assertThat(options.timeout()).isEqualTo(Duration.ofMinutes(2));
	}

}