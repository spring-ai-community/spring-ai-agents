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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * TCK test implementation for LocalSandbox.
 *
 * <p>
 * Tests the LocalSandbox implementation against the standard sandbox TCK test suite.
 * These tests verify that LocalSandbox correctly implements all required Sandbox
 * behaviors.
 * </p>
 *
 * <p>
 * LocalSandbox executes commands on the host system with the specified working directory,
 * so these tests verify local execution behavior.
 * </p>
 */
class LocalSandboxTCKTest extends AbstractSandboxTCK {

	@TempDir
	private Path tempDir;

	@BeforeEach
	void setUp() {
		// Create LocalSandbox with temporary directory
		this.sandbox = new LocalSandbox(tempDir);
	}

}