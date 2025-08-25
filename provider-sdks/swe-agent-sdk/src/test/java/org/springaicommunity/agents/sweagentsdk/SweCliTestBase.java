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

package org.springaicommunity.agents.sweagentsdk;

import org.springaicommunity.agents.sweagentsdk.exceptions.SweCliNotFoundException;
import org.springaicommunity.agents.sweagentsdk.util.SweCliDiscovery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for tests that require SWE Agent CLI to be available.
 *
 * <p>
 * This class automatically discovers the SWE Agent CLI executable and makes it available
 * to subclasses. If SWE Agent CLI cannot be found, all tests will fail with a clear
 * message indicating the issue.
 * </p>
 *
 * <p>
 * Subclasses can access the discovered CLI path via {@link #getSweCliPath()}.
 * </p>
 *
 * <p>
 * <strong>Usage:</strong>
 * </p>
 * <pre>
 * class MySweTest extends SweCliTestBase {
 *
 *     {@literal @}Test
 *     void testSomething() {
 *         // SWE Agent CLI is guaranteed to be available here
 *         String swePath = getSweCliPath();
 *         // ... test implementation
 *     }
 * }
 * </pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SweCliTestBase {

	private static final Logger logger = LoggerFactory.getLogger(SweCliTestBase.class);

	private static String sweCliPath;

	/**
	 * Discovers SWE Agent CLI before any tests run. If discovery fails, all tests in
	 * subclasses will fail with a clear error message.
	 */
	@BeforeAll
	static void discoverSweCli() {
		try {
			sweCliPath = SweCliDiscovery.findSweCommand();
			if (!SweCliDiscovery.isCommandAvailable(sweCliPath)) {
				throw new SweCliNotFoundException("SWE Agent CLI found but not functional at: " + sweCliPath);
			}
			logger.info("SWE Agent CLI tests will use executable at: {}", sweCliPath);
		}
		catch (Exception e) {
			String errorMsg = "SWE Agent CLI Integration Tests Failed: " + e.getMessage();
			logger.error(errorMsg);

			// Throw a runtime exception that will cause all tests to fail with a clear
			// message
			throw new SweCliNotAvailableException(errorMsg);
		}
	}

	/**
	 * Gets the discovered SWE Agent CLI executable path.
	 * @return the path to SWE Agent CLI executable
	 * @throws IllegalStateException if SWE Agent CLI discovery hasn't been performed yet
	 */
	protected static String getSweCliPath() {
		if (sweCliPath == null) {
			throw new IllegalStateException("SWE Agent CLI path not discovered. Ensure @BeforeAll method has run.");
		}
		return sweCliPath;
	}

	/**
	 * Checks if SWE Agent CLI is available for testing.
	 * @return true if SWE Agent CLI is available, false otherwise
	 */
	protected static boolean isSweCliAvailable() {
		return sweCliPath != null;
	}

	/**
	 * Gets the working directory for test execution. Can be overridden by subclasses if
	 * needed.
	 * @return the working directory path
	 */
	protected Path workingDirectory() {
		return Paths.get(".");
	}

	/**
	 * Exception thrown when SWE Agent CLI is not available for testing. This is a runtime
	 * exception that will cause test execution to fail.
	 */
	public static class SweCliNotAvailableException extends RuntimeException {

		public SweCliNotAvailableException(String message) {
			super(message);
		}

	}

}