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
package org.springaicommunity.agents.claude.config;

import org.mockito.Mockito;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for Spring-idiomatic sandbox testing.
 *
 * <p>
 * Usage in tests: <pre>
 * &#64;Import(MockSandboxConfiguration.class)
 * class MyAgentTest {
 *     &#64;Autowired
 *     private Sandbox mockSandbox;
 *
 *     &#64;Test
 *     void testWithMockSandbox() {
 *         // Configure mock behavior
 *         when(mockSandbox.exec(any())).thenReturn(mockResult);
 *         // ... test logic
 *     }
 * }
 * </pre>
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@TestConfiguration
public class MockSandboxConfiguration {

	/**
	 * Creates a mock sandbox for unit testing. The @Primary annotation ensures this mock
	 * takes precedence over auto-configured sandboxes.
	 * @return mocked sandbox instance
	 */
	@Bean
	@Primary
	public Sandbox mockSandbox() {
		return Mockito.mock(Sandbox.class);
	}

}