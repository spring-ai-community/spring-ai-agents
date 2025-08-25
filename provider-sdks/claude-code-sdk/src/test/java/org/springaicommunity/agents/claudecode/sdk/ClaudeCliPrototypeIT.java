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

package org.springaicommunity.agents.claudecode.sdk;

import org.springaicommunity.agents.claudecode.sdk.test.ClaudeCliTestBase;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Integration test for ClaudeCliPrototype.
 *
 * <p>
 * This test extends {@link ClaudeCliTestBase} which automatically discovers Claude CLI
 * and ensures all tests fail gracefully with a clear message if Claude CLI is not
 * available.
 * </p>
 */
class ClaudeCliPrototypeIT extends ClaudeCliTestBase {

	@Test
	void testBasicExecution() throws Exception {
		Path workingDir = Path.of(System.getProperty("user.dir"));
		ClaudeCliPrototype prototype = new ClaudeCliPrototype(workingDir, getClaudeCliPath());

		// Claude CLI is guaranteed to be available here due to ClaudeCliTestBase
		prototype.testBasicExecution();
	}

	@Test
	void testStreamingOutput() throws Exception {
		Path workingDir = Path.of(System.getProperty("user.dir"));
		ClaudeCliPrototype prototype = new ClaudeCliPrototype(workingDir, getClaudeCliPath());

		prototype.testStreamingOutput("What is 1+1?");
	}

	@Test
	void testRealTimeStreaming() throws Exception {
		Path workingDir = Path.of(System.getProperty("user.dir"));
		ClaudeCliPrototype prototype = new ClaudeCliPrototype(workingDir, getClaudeCliPath());

		prototype.testRealTimeStreaming("Hello, world!");
	}

}