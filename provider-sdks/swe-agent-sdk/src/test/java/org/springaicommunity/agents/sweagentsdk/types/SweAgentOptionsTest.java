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

	@Test
	void testNewOptionsDefaults() {
		SweAgentOptions options = new SweAgentOptions();

		// Test new option defaults
		assertThat(options.isYoloMode()).isTrue();
		assertThat(options.isExitImmediately()).isTrue();
		assertThat(options.getCostLimit()).isNull();
		assertThat(options.getConfigPath()).isNull();
		assertThat(options.getOutputPath()).isNull();
		assertThat(options.isVisual()).isFalse();
		assertThat(options.getSystemPrompt()).isNull();
		assertThat(options.getModelParameters()).isEmpty();
		assertThat(options.getAdditionalArgs()).isEmpty();
	}

	@Test
	void testNewOptionsBuilder() {
		SweAgentOptions options = SweAgentOptions.builder()
			.yoloMode(false)
			.exitImmediately(false)
			.costLimit(5.0)
			.visual(true)
			.systemPrompt("Custom prompt")
			.addModelParameter("temperature", 0.7)
			.addEnvironmentVariable("API_KEY", "secret")
			.addAdditionalArg("--debug")
			.build();

		assertThat(options.isYoloMode()).isFalse();
		assertThat(options.isExitImmediately()).isFalse();
		assertThat(options.getCostLimit()).isEqualTo(5.0);
		assertThat(options.isVisual()).isTrue();
		assertThat(options.getSystemPrompt()).isEqualTo("Custom prompt");
		assertThat(options.getModelParameters()).containsEntry("temperature", 0.7);
		assertThat(options.getEnvironmentVariables()).containsEntry("API_KEY", "secret");
		assertThat(options.getAdditionalArgs()).contains("--debug");
	}

	@Test
	void testFactoryMethods() {
		// Test defaultOptions
		SweAgentOptions defaultOpts = SweAgentOptions.defaultOptions();
		assertThat(defaultOpts.isYoloMode()).isTrue();
		assertThat(defaultOpts.isExitImmediately()).isTrue();

		// Test fastResponse
		SweAgentOptions fastOpts = SweAgentOptions.fastResponse();
		assertThat(fastOpts.getModel()).isEqualTo("gpt-4o-mini");
		assertThat(fastOpts.getTimeout()).isEqualTo(Duration.ofMinutes(2));
		assertThat(fastOpts.getMaxIterations()).isEqualTo(10);

		// Test highQuality
		SweAgentOptions qualityOpts = SweAgentOptions.highQuality();
		assertThat(qualityOpts.getModel()).isEqualTo("gpt-4o");
		assertThat(qualityOpts.getTimeout()).isEqualTo(Duration.ofMinutes(10));
		assertThat(qualityOpts.getMaxIterations()).isEqualTo(50);

		// Test development
		SweAgentOptions devOpts = SweAgentOptions.development();
		assertThat(devOpts.isVerbose()).isTrue();
		assertThat(devOpts.getCostLimit()).isEqualTo(1.0);

		// Test supervised
		SweAgentOptions supervisedOpts = SweAgentOptions.supervised();
		assertThat(supervisedOpts.isYoloMode()).isFalse();
		assertThat(supervisedOpts.isExitImmediately()).isFalse();
	}

	@Test
	void testBuilderTimeoutConvenience() {
		SweAgentOptions options1 = SweAgentOptions.builder().timeoutMinutes(5).build();
		assertThat(options1.getTimeout()).isEqualTo(Duration.ofMinutes(5));

		SweAgentOptions options2 = SweAgentOptions.builder().timeoutSeconds(30).build();
		assertThat(options2.getTimeout()).isEqualTo(Duration.ofSeconds(30));
	}

}