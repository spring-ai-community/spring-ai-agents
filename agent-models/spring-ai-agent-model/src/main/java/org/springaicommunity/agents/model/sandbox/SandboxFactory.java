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

import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating sandbox instances with auto-detection of Docker availability.
 *
 * <p>
 * This factory attempts to create a DockerSandbox by default, falling back to
 * LocalSandbox if Docker is not available. This provides secure-by-default behavior while
 * still allowing operation in environments without Docker.
 */
public final class SandboxFactory {

	private static final Logger logger = LoggerFactory.getLogger(SandboxFactory.class);

	private static final String DEFAULT_DOCKER_IMAGE = "ghcr.io/spring-ai-community/agents-runtime:latest";

	private SandboxFactory() {
		// Utility class
	}

	/**
	 * Create a sandbox with auto-detection of Docker availability.
	 * @return DockerSandbox if Docker is available, LocalSandbox otherwise
	 */
	public static Sandbox createSandbox() {
		return createSandbox(DEFAULT_DOCKER_IMAGE, List.of());
	}

	/**
	 * Create a sandbox with the specified Docker image and customizers.
	 * @param dockerImage the Docker image to use (ignored if Docker unavailable)
	 * @param customizers list of execution customizers
	 * @return DockerSandbox if Docker is available, LocalSandbox otherwise
	 */
	public static Sandbox createSandbox(String dockerImage, List<ExecSpecCustomizer> customizers) {
		if (isDockerAvailable()) {
			try {
				logger.info("Docker detected - creating DockerSandbox with image: {}", dockerImage);
				return new DockerSandbox(dockerImage, customizers);
			}
			catch (Exception e) {
				logger.warn("Failed to create DockerSandbox despite Docker being available: {}", e.getMessage());
				logger.warn("Falling back to LocalSandbox - WARNING: NO ISOLATION PROVIDED");
				return new LocalSandbox(Path.of(System.getProperty("user.dir")), customizers);
			}
		}
		else {
			logger.warn("Docker not available - creating LocalSandbox");
			logger.warn("WARNING: NO ISOLATION PROVIDED - commands execute directly on host system");
			return new LocalSandbox(Path.of(System.getProperty("user.dir")), customizers);
		}
	}

	/**
	 * Create a DockerSandbox explicitly, failing if Docker is not available.
	 * @return DockerSandbox instance
	 * @throws IllegalStateException if Docker is not available
	 */
	public static DockerSandbox createDockerSandbox() {
		return createDockerSandbox(DEFAULT_DOCKER_IMAGE, List.of());
	}

	/**
	 * Create a DockerSandbox explicitly with the specified image and customizers.
	 * @param dockerImage the Docker image to use
	 * @param customizers list of execution customizers
	 * @return DockerSandbox instance
	 * @throws IllegalStateException if Docker is not available
	 */
	public static DockerSandbox createDockerSandbox(String dockerImage, List<ExecSpecCustomizer> customizers) {
		if (!isDockerAvailable()) {
			throw new IllegalStateException("Docker is not available - cannot create DockerSandbox");
		}
		return new DockerSandbox(dockerImage, customizers);
	}

	/**
	 * Create a LocalSandbox explicitly.
	 * @return LocalSandbox instance
	 */
	public static LocalSandbox createLocalSandbox() {
		return createLocalSandbox(Path.of(System.getProperty("user.dir")), List.of());
	}

	/**
	 * Create a LocalSandbox explicitly with the specified working directory and
	 * customizers.
	 * @param workingDirectory the working directory for command execution
	 * @param customizers list of execution customizers
	 * @return LocalSandbox instance
	 */
	public static LocalSandbox createLocalSandbox(Path workingDirectory, List<ExecSpecCustomizer> customizers) {
		return new LocalSandbox(workingDirectory, customizers);
	}

	/**
	 * Check if Docker is available on the system.
	 * @return true if Docker daemon is accessible, false otherwise
	 */
	public static boolean isDockerAvailable() {
		try {
			// Try to create a simple container to test Docker availability
			// This is a lightweight test that doesn't actually start a container
			org.testcontainers.DockerClientFactory.instance().client();
			return true;
		}
		catch (Exception e) {
			logger.debug("Docker availability check failed: {}", e.getMessage());
			return false;
		}
	}

}