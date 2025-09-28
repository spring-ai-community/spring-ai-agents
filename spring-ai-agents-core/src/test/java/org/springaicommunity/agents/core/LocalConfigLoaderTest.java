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

package org.springaicommunity.agents.core;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LocalConfigLoader CLI parsing and edge cases.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
class LocalConfigLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void testNoArguments() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			LocalConfigLoader.load(new String[0]);
		});
		assertEquals("Usage: launcher <agentId> key=value [key2=value2 ...]", ex.getMessage());
	}

	@Test
	void testInvalidAgentId() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			LocalConfigLoader.load(new String[] { "123invalid" });
		});
		assertEquals("Unknown agent: 123invalid. Check agent availability.", ex.getMessage());
	}

	@Test
	void testInvalidToken() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			LocalConfigLoader.load(new String[] { "hello-world", "invalid-token" });
		});
		assertEquals("Expected key=value token, got: invalid-token", ex.getMessage());
	}

	@Test
	void testEmptyValue() throws Exception {
		// Create minimal hello-world.yaml for agent spec
		createHelloWorldAgentSpec();

		LauncherSpec spec = LocalConfigLoader.load(new String[] { "hello-world", "key=" });
		assertEquals("", spec.inputs().get("key"));
	}

	@Test
	void testDuplicateKeysLastWins() throws Exception {
		createHelloWorldAgentSpec();

		LauncherSpec spec = LocalConfigLoader
			.load(new String[] { "hello-world", "key=first", "key=second", "key=last" });
		assertEquals("last", spec.inputs().get("key"));
	}

	@Test
	void testValueWithEquals() throws Exception {
		createHelloWorldAgentSpec();

		LauncherSpec spec = LocalConfigLoader.load(new String[] { "hello-world", "url=http://example.com?a=b=c" });
		assertEquals("http://example.com?a=b=c", spec.inputs().get("url"));
	}

	@Test
	@Disabled("Test requires filesystem isolation - needs test environment setup")
	void testRunspecWithBlankWorkingDirectory() throws Exception {
		createHelloWorldAgentSpec();

		// Create runspec with blank workingDirectory in .agents directory
		Path agentsDir = tempDir.resolve(".agents");
		Files.createDirectories(agentsDir);
		Path runspec = agentsDir.resolve("run.yaml");
		Files.writeString(runspec, "workingDirectory: \"\"\nenv:\n  test: value\n");

		// Change working directory to tempDir for test
		String originalDir = System.getProperty("user.dir");
		System.setProperty("user.dir", tempDir.toString());
		try {
			LauncherSpec spec = LocalConfigLoader.load(new String[] { "hello-world" });
			assertEquals(Path.of("."), spec.cwd()); // Should use default
			assertEquals("value", spec.env().get("test"));
		}
		finally {
			System.setProperty("user.dir", originalDir);
		}
	}

	@Test
	@Disabled("Test requires filesystem isolation - needs test environment setup")
	void testRunspecWithNonMapRoot() throws Exception {
		createHelloWorldAgentSpec();

		// Create invalid runspec in .agents directory
		Path agentsDir = tempDir.resolve(".agents");
		Files.createDirectories(agentsDir);
		Path runspec = agentsDir.resolve("run.yaml");
		Files.writeString(runspec, "- not a mapping\n- invalid structure\n");

		// Change working directory to tempDir for test
		String originalDir = System.getProperty("user.dir");
		System.setProperty("user.dir", tempDir.toString());
		try {
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
				LocalConfigLoader.load(new String[] { "hello-world" });
			});
			assertTrue(ex.getMessage().contains("YAML root must be a mapping"));
		}
		finally {
			System.setProperty("user.dir", originalDir);
		}
	}

	private void createHelloWorldAgentSpec() throws Exception {
		// Create .agents directory and hello-world.yaml in current working directory
		Path currentDir = Path.of(System.getProperty("user.dir"));
		Path agentsDir = currentDir.resolve(".agents");
		Files.createDirectories(agentsDir);
		Path agentSpec = agentsDir.resolve("hello-world.yaml");
		Files.writeString(agentSpec, """
				id: hello-world
				version: 0.1
				inputs:
				  path:
				    type: string
				    required: true
				  content:
				    type: string
				    default: "HelloWorld"
				""");
	}

}