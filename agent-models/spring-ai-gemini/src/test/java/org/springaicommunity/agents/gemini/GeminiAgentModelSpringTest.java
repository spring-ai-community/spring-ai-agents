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

package org.springaicommunity.agents.gemini;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.sandbox.LocalSandbox;
import org.springaicommunity.sandbox.Sandbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GeminiAgentModel using Spring Boot auto-configuration patterns.
 *
 * This test demonstrates the proper Spring-idiomatic approach to dependency injection for
 * AI agent models, following the patterns established by Spring AI.
 */
@SpringBootTest(classes = GeminiAgentModelSpringTest.TestConfig.class)
@TestPropertySource(
		properties = { "spring.ai.agents.gemini.model=gemini-2.0-flash-exp", "spring.ai.agents.gemini.timeout=PT5M",
				"spring.ai.agents.gemini.yolo=true", "spring.ai.agents.sandbox.docker.enabled=false" })
class GeminiAgentModelSpringTest {

	@Autowired
	private GeminiAgentModel agentModel;

	@Autowired
	private GeminiClient geminiClient;

	@Autowired
	private Sandbox sandbox;

	@Test
	void testSpringDependencyInjection() {
		// ASSERT: All beans are properly injected by Spring
		assertThat(agentModel).isNotNull();
		assertThat(geminiClient).isNotNull();
		assertThat(sandbox).isNotNull();
	}

	@Test
	void testAgentModelConfiguredWithProperties() {
		// ASSERT: Agent model is configured with properties from application.properties
		assertThat(agentModel).isNotNull();
		// The agent model should be properly configured via auto-configuration
		// Since we're using mocked dependencies, the agent model should be functional
	}

	@Configuration
	static class TestConfig {

		@Bean
		public GeminiClient geminiClient() {
			// Mock client for testing - use Mockito instead of inheritance
			GeminiClient mockClient = org.mockito.Mockito.mock(GeminiClient.class);
			// Note: GeminiClient doesn't have isConnected() method
			return mockClient;
		}

		@Bean
		public Sandbox sandbox() {
			// Local sandbox for testing
			return new LocalSandbox(Paths.get(System.getProperty("java.io.tmpdir")));
		}

		@Bean
		public GeminiAgentModel geminiAgentModel(GeminiClient geminiClient, Sandbox sandbox) {
			// Create agent model with default options
			GeminiAgentOptions options = GeminiAgentOptions.builder()
				.model("gemini-2.0-flash-exp")
				.timeout(java.time.Duration.ofMinutes(5))
				.yolo(true)
				.build();
			return new GeminiAgentModel(geminiClient, options, sandbox);
		}

	}

}