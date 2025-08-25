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

package org.springaicommunity.agents.sweagentsdk.types;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SweAgentOptions.
 */
class SweAgentOptionsTest {

	@Test
	void testDefaultConstructor() {
		SweAgentOptions options = new SweAgentOptions();

		assertThat(options.getModel()).isNull();
		assertThat(options.getTimeout()).isEqualTo(Duration.ofMinutes(5));
		assertThat(options.getWorkingDirectory()).isNull();
		assertThat(options.getEnvironmentVariables()).isEmpty();
		assertThat(options.getExecutablePath()).isNull();
		assertThat(options.getMaxIterations()).isEqualTo(20);
		assertThat(options.isVerbose()).isFalse();
	}

	@Test
	void testModelConstructor() {
		String model = "claude-3-5-sonnet";
		SweAgentOptions options = new SweAgentOptions(model);

		assertThat(options.getModel()).isEqualTo(model);
		assertThat(options.getTimeout()).isEqualTo(Duration.ofMinutes(5));
	}

	@Test
	void testBuilder() {
		String model = "gpt-4";
		Duration timeout = Duration.ofMinutes(10);
		String workingDir = "/tmp/test";
		Map<String, String> envVars = Map.of("KEY", "value");
		String execPath = "/usr/local/bin/mini";
		int maxIterations = 30;

		SweAgentOptions options = SweAgentOptions.builder()
			.model(model)
			.timeout(timeout)
			.workingDirectory(workingDir)
			.environmentVariables(envVars)
			.executablePath(execPath)
			.maxIterations(maxIterations)
			.verbose(true)
			.build();

		assertThat(options.getModel()).isEqualTo(model);
		assertThat(options.getTimeout()).isEqualTo(timeout);
		assertThat(options.getWorkingDirectory()).isEqualTo(workingDir);
		assertThat(options.getEnvironmentVariables()).isEqualTo(envVars);
		assertThat(options.getExecutablePath()).isEqualTo(execPath);
		assertThat(options.getMaxIterations()).isEqualTo(maxIterations);
		assertThat(options.isVerbose()).isTrue();
	}

	@Test
	void testSetters() {
		SweAgentOptions options = new SweAgentOptions();

		options.setModel("claude-3-5-sonnet");
		options.setTimeout(Duration.ofMinutes(3));
		options.setWorkingDirectory("/home/user");
		options.setEnvironmentVariables(Map.of("TEST", "value"));
		options.setExecutablePath("/opt/mini");
		options.setMaxIterations(15);
		options.setVerbose(true);

		assertThat(options.getModel()).isEqualTo("claude-3-5-sonnet");
		assertThat(options.getTimeout()).isEqualTo(Duration.ofMinutes(3));
		assertThat(options.getWorkingDirectory()).isEqualTo("/home/user");
		assertThat(options.getEnvironmentVariables()).containsEntry("TEST", "value");
		assertThat(options.getExecutablePath()).isEqualTo("/opt/mini");
		assertThat(options.getMaxIterations()).isEqualTo(15);
		assertThat(options.isVerbose()).isTrue();
	}

	@Test
	void testEnvironmentVariablesNullHandling() {
		SweAgentOptions options = new SweAgentOptions();

		options.setEnvironmentVariables(null);
		assertThat(options.getEnvironmentVariables()).isEmpty();
	}

}