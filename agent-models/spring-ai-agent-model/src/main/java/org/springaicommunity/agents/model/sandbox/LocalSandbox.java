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
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

/**
 * Sandbox implementation that executes commands directly on the host system.
 *
 * <p>
 * <strong>WARNING:</strong> This implementation provides NO isolation and should only be
 * used when Docker is not available. Commands executed through this sandbox can access
 * and modify the host system.
 *
 * <p>
 * This is intended as a fallback option for development or testing scenarios where Docker
 * is not available. Production deployments should use {@link DockerSandbox}.
 */
public final class LocalSandbox implements Sandbox {

	private static final Logger logger = LoggerFactory.getLogger(LocalSandbox.class);

	private final Path workingDirectory;

	private final List<ExecSpecCustomizer> customizers;

	private volatile boolean closed = false;

	/**
	 * Creates a LocalSandbox with the current working directory.
	 */
	public LocalSandbox() {
		this(Path.of(System.getProperty("user.dir")), List.of());
	}

	/**
	 * Creates a LocalSandbox with the specified working directory.
	 * @param workingDirectory the working directory for command execution
	 */
	public LocalSandbox(Path workingDirectory) {
		this(workingDirectory, List.of());
	}

	/**
	 * Creates a LocalSandbox with the specified working directory and customizers.
	 * @param workingDirectory the working directory for command execution
	 * @param customizers list of customizers to apply before execution
	 */
	public LocalSandbox(Path workingDirectory, List<ExecSpecCustomizer> customizers) {
		this.workingDirectory = workingDirectory;
		this.customizers = List.copyOf(customizers);
		logger.warn("LocalSandbox created - NO ISOLATION PROVIDED. Commands execute directly on host system.");
	}

	@Override
	public Path workDir() {
		return workingDirectory;
	}

	@Override
	public ExecResult exec(ExecSpec spec) {
		if (closed) {
			throw new IllegalStateException("Sandbox is closed");
		}

		var startTime = Instant.now();
		var customizedSpec = applyCustomizers(spec);
		var command = customizedSpec.command();

		if (command.isEmpty()) {
			throw new IllegalArgumentException("Command cannot be null or empty");
		}

		// Handle shell commands
		List<String> finalCommand = processCommand(command);

		try {
			// Use zt-exec for robust process execution
			ProcessExecutor executor = new ProcessExecutor().command(finalCommand)
				.directory(workingDirectory.toFile())
				.readOutput(true)
				.destroyOnExit();

			logger.info("LocalSandbox executing command in directory: {}", workingDirectory);
			logger.info("LocalSandbox command size: {} args", finalCommand.size());
			for (int i = 0; i < finalCommand.size(); i++) {
				String arg = finalCommand.get(i);
				if (i == finalCommand.size() - 1 && arg.length() > 200) {
					logger.info("LocalSandbox arg[{}]: {} ... (truncated, length={})", i, arg.substring(0, 200),
							arg.length());
				}
				else {
					logger.info("LocalSandbox arg[{}]: {}", i, arg);
				}
			}

			// Apply environment variables
			if (!customizedSpec.env().isEmpty()) {
				logger.info("LocalSandbox environment variables: {}", customizedSpec.env().keySet());
				executor.environment(customizedSpec.env());
			}

			// Handle timeout
			if (customizedSpec.timeout() != null) {
				logger.info("LocalSandbox timeout: {}", customizedSpec.timeout());
				executor.timeout(customizedSpec.timeout().toMillis(), TimeUnit.MILLISECONDS);
			}

			logger.info("LocalSandbox about to execute command...");
			ProcessResult result = executor.execute();
			logger.info("LocalSandbox command completed with exit code: {}", result.getExitValue());
			Duration duration = Duration.between(startTime, Instant.now());

			return new ExecResult(result.getExitValue(), result.outputUTF8(), duration);
		}
		catch (org.zeroturnaround.exec.InvalidExitValueException e) {
			// zt-exec throws this for non-zero exit codes, but we want to return the
			// result anyway
			Duration duration = Duration.between(startTime, Instant.now());
			return new ExecResult(e.getExitValue(), e.getResult().outputUTF8(), duration);
		}
		catch (java.util.concurrent.TimeoutException e) {
			throw new org.springaicommunity.agents.model.sandbox.TimeoutException(
					"Command timed out after " + customizedSpec.timeout(), customizedSpec.timeout());
		}
		catch (Exception e) {
			throw new SandboxException("Failed to execute command", e);
		}
	}

	private List<String> processCommand(List<String> command) {
		// Handle special shell command marker
		if (command.size() >= 2 && "__SHELL_COMMAND__".equals(command.get(0))) {
			String shellCmd = command.get(1);
			// Use platform-appropriate shell
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				return List.of("cmd", "/c", shellCmd);
			}
			else {
				return List.of("bash", "-c", shellCmd);
			}
		}
		return command;
	}

	private ExecSpec applyCustomizers(ExecSpec spec) {
		ExecSpec customizedSpec = spec;
		for (ExecSpecCustomizer customizer : customizers) {
			customizedSpec = customizer.customize(customizedSpec);
		}
		return customizedSpec;
	}

	@Override
	public void close() {
		closed = true;
		logger.debug("LocalSandbox closed");
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Gets the list of customizers used by this sandbox.
	 * @return immutable list of customizers
	 */
	public List<ExecSpecCustomizer> getCustomizers() {
		return customizers;
	}

	@Override
	public String toString() {
		return String.format("LocalSandbox{workDir=%s, customizers=%d, closed=%s}", workingDirectory,
				customizers.size(), closed);
	}

}