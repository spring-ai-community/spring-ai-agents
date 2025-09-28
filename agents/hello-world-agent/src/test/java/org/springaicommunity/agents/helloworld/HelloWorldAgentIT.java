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

package org.springaicommunity.agents.helloworld;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for HelloWorldAgent via JBang agents.java launcher. Tests the
 * end-to-end JBang runner functionality with the hello-world agent.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class HelloWorldAgentIT {

	@TempDir
	Path tempDir;

	/**
	 * Find the agents.java file in the project root.
	 */
	private Path findAgentsJavaFile() {
		// Start from current directory and walk up to find agents.java
		Path current = Path.of(System.getProperty("user.dir"));
		for (int i = 0; i < 5; i++) { // Look up to 5 levels up
			Path agentsJava = current.resolve("agents.java");
			if (Files.exists(agentsJava)) {
				return agentsJava;
			}
			current = current.getParent();
			if (current == null) {
				break;
			}
		}
		throw new RuntimeException("Could not find agents.java file in project root");
	}

	@Test
	void testJBangAgentsLauncherWithHelloWorld() throws Exception {
		// Arrange
		String testContent = "Hello from integration test!";
		String fileName = "integration-test.txt";
		Path expectedFile = tempDir.resolve(fileName);

		// Find the agents.java file - it should be in the project root
		Path agentsJavaPath = findAgentsJavaFile();

		// Act - Execute JBang agents.java with hello-world agent using zt-exec
		ProcessResult result = new ProcessExecutor()
			.command("jbang", agentsJavaPath.toString(), "--agent", "hello-world", "--path", fileName, "--content",
					testContent, "--workdir", tempDir.toString())
			.directory(tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		// Debug output if test fails
		if (result.getExitValue() != 0) {
			System.out.println("JBang output:");
			System.out.println(result.outputUTF8());
		}

		// Assert
		assertEquals(0, result.getExitValue(),
				"JBang process should exit successfully. Output: " + result.outputUTF8());

		// Verify file was created with correct content
		assertTrue(Files.exists(expectedFile), "Output file should be created");
		String actualContent = Files.readString(expectedFile);
		assertEquals(testContent, actualContent, "File content should match expected");
	}

	@Test
	void testJBangAgentsLauncherWithDefaultContent() throws Exception {
		// Arrange
		String fileName = "default-content-test.txt";
		Path expectedFile = tempDir.resolve(fileName);

		// Find the agents.java file - it should be in the project root
		Path agentsJavaPath = findAgentsJavaFile();

		// Act - Execute JBang agents.java with hello-world agent (using default content)
		ProcessResult result = new ProcessExecutor()
			.command("jbang", agentsJavaPath.toString(), "--agent", "hello-world", "--path", fileName, "--workdir",
					tempDir.toString())
			.directory(tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		// Debug output if test fails
		if (result.getExitValue() != 0) {
			System.out.println("JBang output:");
			System.out.println(result.outputUTF8());
		}

		// Assert
		assertEquals(0, result.getExitValue(),
				"JBang process should exit successfully. Output: " + result.outputUTF8());

		// Verify file was created with default content
		assertTrue(Files.exists(expectedFile), "Output file should be created");
		String actualContent = Files.readString(expectedFile);
		assertEquals("HelloWorld", actualContent, "File content should use default value");
	}

	@Test
	void testJBangAgentsLauncherMissingRequiredInput() throws Exception {
		// Find the agents.java file - it should be in the project root
		Path agentsJavaPath = findAgentsJavaFile();

		// Act - Execute JBang agents.java with hello-world agent but missing required
		// 'path' input
		ProcessResult result = new ProcessExecutor()
			.command("jbang", agentsJavaPath.toString(), "--agent", "hello-world", "--content", "test content",
					"--workdir", tempDir.toString())
			.directory(tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		// Assert - Should fail due to missing required input
		assertNotEquals(0, result.getExitValue(), "JBang process should exit with error code");
	}

	@Test
	void testJBangAgentsLauncherUnknownAgent() throws Exception {
		// Find the agents.java file - it should be in the project root
		Path agentsJavaPath = findAgentsJavaFile();

		// Act - Execute JBang agents.java with unknown agent
		ProcessResult result = new ProcessExecutor()
			.command("jbang", agentsJavaPath.toString(), "--agent", "unknown-agent", "--workdir", tempDir.toString())
			.directory(tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		// Assert - Should fail due to unknown agent
		assertNotEquals(0, result.getExitValue(), "JBang process should exit with error code");
	}

}