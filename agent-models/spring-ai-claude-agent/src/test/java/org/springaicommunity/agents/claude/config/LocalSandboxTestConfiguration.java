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

import org.springaicommunity.agents.model.sandbox.LocalSandbox;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.nio.file.Paths;

/**
 * Test configuration for local sandbox testing.
 *
 * <p>
 * Usage in integration tests: <pre>
 * &#64;SpringBootTest
 * &#64;Import(LocalSandboxTestConfiguration.class)
 * class MyIntegrationTest {
 *     &#64;Autowired
 *     private Sandbox sandbox; // Will be LocalSandbox
 *
 *     &#64;Test
 *     void testWithLocalSandbox() {
 *         // Test with real local execution
 *     }
 * }
 * </pre>
 *
 * <p>
 * Alternatively, use test properties: <pre>
 * &#64;TestPropertySource(properties = "spring.ai.agents.sandbox.docker.enabled=false")
 * </pre>
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@TestConfiguration
public class LocalSandboxTestConfiguration {

	/**
	 * Creates a local sandbox for integration testing. Uses the system temp directory for
	 * safe testing isolation.
	 * @return configured local sandbox
	 */
	@Bean
	@Primary
	public Sandbox localSandbox() {
		return new LocalSandbox(Paths.get(System.getProperty("java.io.tmpdir")));
	}

}