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

package org.springaicommunity.agents.model.sandbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

/**
 * TCK test implementation for DockerSandbox.
 *
 * <p>
 * Tests the DockerSandbox implementation against the standard sandbox TCK test suite.
 * These tests verify that DockerSandbox correctly implements all required Sandbox
 * behaviors using Docker containers for isolation.
 * </p>
 *
 * <p>
 * Run with: mvn test -Dtest=DockerSandboxTCKTest -Dsandbox.infrastructure.test=true
 * </p>
 *
 * <p>
 * Requires Docker to be available and the agents-runtime image to be accessible.
 * </p>
 */
@EnabledIfSystemProperty(named = "sandbox.infrastructure.test", matches = "true")
class DockerSandboxTCKTest extends AbstractSandboxTCK {

	@BeforeEach
	void setUp() {
		// Create DockerSandbox with agents-runtime image
		this.sandbox = new DockerSandbox("ghcr.io/spring-ai-community/agents-runtime:latest", List.of());
	}

}