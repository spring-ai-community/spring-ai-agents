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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Sandbox implementation that executes commands in Docker containers for strong
 * isolation.
 *
 * <p>
 * Supports {@link ExecSpecCustomizer}s for last-mile command and environment
 * customization. Each execution applies all customizers in sequence before running the
 * command in the container.
 *
 * <p>
 * The container runs with a long-lived "sleep infinity" process, allowing multiple
 * command executions within the same container environment.
 */
public final class DockerSandbox implements Sandbox {

	private static final Logger logger = LoggerFactory.getLogger(DockerSandbox.class);

	private static final String DEFAULT_IMAGE = "ghcr.io/spring-ai-community/agents-runtime:latest";

	private final GenericContainer<?> container;

	private final List<ExecSpecCustomizer> customizers;

	private volatile boolean closed = false;

	/**
	 * Creates a DockerSandbox with the default agents runtime image and no customizers.
	 */
	public DockerSandbox() {
		this(DEFAULT_IMAGE, List.of());
	}

	/**
	 * Creates a DockerSandbox with no customizers.
	 * @param baseImage the Docker image to use for the container
	 */
	public DockerSandbox(String baseImage) {
		this(baseImage, List.of());
	}

	/**
	 * Creates a DockerSandbox with the specified customizers.
	 * @param baseImage the Docker image to use for the container
	 * @param customizers list of customizers to apply before execution
	 */
	public DockerSandbox(String baseImage, List<ExecSpecCustomizer> customizers) {
		this.customizers = List.copyOf(customizers);
		this.container = new GenericContainer<>(DockerImageName.parse(baseImage)).withWorkingDirectory("/work")
			.withCommand("sleep", "infinity");

		container.start();
		logger.debug("Started DockerSandbox with image: {} and {} customizers", baseImage, customizers.size());
	}

	@Override
	public Path workDir() {
		return Path.of("/work");
	}

	@Override
	public ExecResult exec(ExecSpec spec) throws IOException, InterruptedException, TimeoutException {
		if (closed) {
			throw new IllegalStateException("Sandbox is closed");
		}

		var startTime = Instant.now();
		var customizedSpec = applyCustomizers(spec);
		var command = customizedSpec.command();
		if (command.isEmpty()) {
			throw new IllegalArgumentException("Command cannot be null or empty");
		}

		// The most robust way to pass arguments to a shell is via positional parameters.
		// We use 'exec "$@"' to replace the shell process with the command,
		// which also ensures the exit code is passed through correctly.
		List<String> finalCommandList = new ArrayList<>();
		finalCommandList.add("bash");
		finalCommandList.add("-lc");
		finalCommandList.add("exec \"$@\""); // The script to execute
		finalCommandList.add("bash"); // This becomes $0 for the script
		finalCommandList.addAll(command); // These become $1, $2, ...

		try {
			var result = this.container.execInContainer(finalCommandList.toArray(new String[0]));
			var duration = Duration.between(startTime, Instant.now());
			String mergedLog = result.getStdout() + result.getStderr();
			return new ExecResult(result.getExitCode(), mergedLog, duration);
		}
		catch (Exception e) {
			throw new IOException("Failed to execute command in container", e);
		}
	}

	private ExecSpec applyCustomizers(ExecSpec spec) {
		ExecSpec customizedSpec = spec;
		for (ExecSpecCustomizer customizer : customizers) {
			customizedSpec = customizer.customize(customizedSpec);
		}
		return customizedSpec;
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}

		closed = true;
		logger.debug("Stopping DockerSandbox container");

		try {
			container.stop();
			logger.debug("Successfully stopped container");
		}
		catch (Exception e) {
			logger.warn("Failed to stop container cleanly", e);
			throw new IOException("Failed to close DockerSandbox", e);
		}
	}

	/**
	 * Gets the list of customizers used by this sandbox.
	 * @return immutable list of customizers
	 */
	public List<ExecSpecCustomizer> getCustomizers() {
		return customizers;
	}

	/** Checks if this sandbox has been closed. */
	@Override
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Gets the Docker container used by this sandbox. Useful for advanced container
	 * operations.
	 * @return the underlying container
	 */
	public GenericContainer<?> getContainer() {
		return container;
	}

	@Override
	public String toString() {
		return String.format("DockerSandbox{image=%s, customizers=%d, closed=%s}", container.getDockerImageName(),
				customizers.size(), closed);
	}

}