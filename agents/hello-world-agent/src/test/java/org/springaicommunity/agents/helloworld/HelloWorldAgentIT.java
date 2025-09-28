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
	 * Get the launcher.java file path in the jbang directory.
	 */
	private Path getLauncherJavaFile() {
		// Find project root by walking up directories looking for jbang/launcher.java
		Path current = Path.of(System.getProperty("user.dir"));
		for (int i = 0; i < 5; i++) {
			Path launcherJava = current.resolve("jbang/launcher.java");
			if (Files.exists(launcherJava)) {
				// Found project root with jbang/launcher.java
				return launcherJava;
			}
			current = current.getParent();
			if (current == null) {
				break;
			}
		}
		throw new RuntimeException("Could not find jbang/launcher.java in project root");
	}

	/**
	 * Get the JBang executable path.
	 */
	private String getJBangExecutable() {
		String jbangHome = System.getenv("JBANG_HOME");
		System.out.println("jbang home " + jbangHome);
		if (jbangHome != null) {
			return jbangHome + "/bin/jbang";
		}
		// Fallback to PATH lookup
		return "jbang";
	}

	@Test
	void testJBangAgentsLauncherWithHelloWorld() throws Exception {
		// Arrange
		String testContent = "Hello from integration test!";
		String fileName = "integration-test.txt";
		Path expectedFile = tempDir.resolve(fileName);

		// Get the launcher.java file in jbang directory
		Path launcherJavaPath = getLauncherJavaFile();

		// Act - Execute JBang launcher.java with hello-world agent using zt-exec
		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world", "path=" + fileName,
					"content=" + testContent)
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

		// Get the launcher.java file in jbang directory
		Path launcherJavaPath = getLauncherJavaFile();

		// Act - Execute JBang launcher.java with hello-world agent (using default
		// content)
		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world", "path=" + fileName)
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
		// Get the launcher.java file in jbang directory
		Path launcherJavaPath = getLauncherJavaFile();

		// Act - Execute JBang launcher.java with hello-world agent but missing required
		// 'path' input
		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world", "content=test content")
			.directory(tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		// Assert - Should fail due to missing required input
		assertNotEquals(0, result.getExitValue(), "JBang process should exit with error code");
	}

	@Test
	void testJBangAgentsLauncherUnknownAgent() throws Exception {
		// Get the launcher.java file in jbang directory
		Path launcherJavaPath = getLauncherJavaFile();

		// Act - Execute JBang launcher.java with unknown agent
		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "unknown-agent")
			.directory(tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		// Assert - Should fail due to unknown agent
		assertNotEquals(0, result.getExitValue(), "JBang process should exit with error code");
	}

}