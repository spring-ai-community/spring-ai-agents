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

package org.springaicommunity.agents.client.advisor.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springframework.core.Ordered;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

/**
 * Context engineering advisor that uses vendir to gather external reference materials
 * before agent execution.
 *
 * <p>
 * This advisor implements the "Select Context" strategy of context engineering by using
 * vendir to declaratively fetch external documentation, API specs, examples, and best
 * practices that can help the agent make better decisions.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * // Create vendir.yml with documentation sources
 * Path vendirConfig = createVendirConfig();
 *
 * // Create advisor
 * VendirContextAdvisor advisor = VendirContextAdvisor.builder()
 *     .vendirConfigPath(vendirConfig)
 *     .contextDirectory(".agent-context/vendir")
 *     .build();
 *
 * // Use with AgentClient
 * AgentClient client = AgentClient.builder(agentModel)
 *     .defaultAdvisor(advisor)
 *     .build();
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see <a href="https://carvel.dev/vendir/">Vendir Documentation</a>
 */
public class VendirContextAdvisor implements AgentCallAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(VendirContextAdvisor.class);

	private final Path vendirConfigPath;

	private final String contextDirectory;

	private final boolean autoCleanup;

	private final long timeoutSeconds;

	private final int order;

	/**
	 * Creates a VendirContextAdvisor with the specified configuration.
	 * @param vendirConfigPath path to vendir.yml configuration file
	 * @param contextDirectory subdirectory within working directory for context files
	 * (default: ".agent-context/vendir")
	 * @param autoCleanup whether to clean up context files after agent execution
	 * (default: false)
	 * @param timeoutSeconds timeout for vendir sync operation (default: 300)
	 * @param order advisor execution order (default: HIGHEST_PRECEDENCE + 100)
	 */
	private VendirContextAdvisor(Path vendirConfigPath, String contextDirectory, boolean autoCleanup,
			long timeoutSeconds, int order) {
		this.vendirConfigPath = vendirConfigPath;
		this.contextDirectory = contextDirectory != null ? contextDirectory : ".agent-context/vendir";
		this.autoCleanup = autoCleanup;
		this.timeoutSeconds = timeoutSeconds;
		this.order = order;
	}

	@Override
	public AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain) {
		Path workingDir = request.workingDirectory();
		Path contextPath = workingDir.resolve(this.contextDirectory);

		logger.info("VendirContextAdvisor: Gathering external context via vendir");
		logger.debug("Working directory: {}", workingDir);
		logger.debug("Context directory: {}", contextPath);
		logger.debug("Vendir config: {}", this.vendirConfigPath);

		try {
			// Ensure context directory exists
			Files.createDirectories(contextPath);

			// Run vendir sync
			VendirSyncResult syncResult = syncVendir(workingDir, contextPath);

			// Add metadata to request context
			request.context().put("vendir.context.path", contextPath.toString());
			request.context().put("vendir.context.success", syncResult.success);
			request.context().put("vendir.context.output", syncResult.output);
			if (!syncResult.success) {
				request.context().put("vendir.context.error", syncResult.error);
			}

			logger.info("Vendir sync completed. Success: {}", syncResult.success);

			// Execute agent with enriched context
			AgentClientResponse response = chain.nextCall(request);

			// Optional cleanup
			if (this.autoCleanup && syncResult.success) {
				logger.debug("Auto-cleanup enabled, removing context directory: {}", contextPath);
				deleteRecursively(contextPath);
			}

			// Add context gathering metadata to response
			response.context().put("vendir.context.gathered", syncResult.success);
			return response;

		}
		catch (IOException e) {
			logger.error("Failed to prepare context directory", e);
			request.context().put("vendir.context.success", false);
			request.context().put("vendir.context.error", e.getMessage());

			// Continue execution even if context gathering fails
			return chain.nextCall(request);
		}
	}

	private VendirSyncResult syncVendir(Path workingDir, Path contextPath) {
		List<String> command = new ArrayList<>();
		command.add("vendir");
		command.add("sync");
		command.add("--chdir");
		command.add(contextPath.toString());

		// If vendirConfigPath is absolute, use it directly; otherwise resolve relative
		// to working dir
		Path configPath = this.vendirConfigPath.isAbsolute() ? this.vendirConfigPath
				: workingDir.resolve(this.vendirConfigPath);

		command.add("--file");
		command.add(configPath.toString());

		logger.debug("Executing vendir command: {}", String.join(" ", command));

		try {
			ProcessResult result = new ProcessExecutor().command(command)
				.timeout(this.timeoutSeconds, TimeUnit.SECONDS)
				.readOutput(true)
				.execute();

			String output = result.outputUTF8();
			int exitCode = result.getExitValue();
			boolean success = exitCode == 0;

			if (success) {
				logger.info("Vendir sync completed successfully");
				logger.debug("Vendir output:\n{}", output);
			}
			else {
				logger.warn("Vendir sync failed with exit code: {}", exitCode);
				logger.debug("Vendir output:\n{}", output);
			}

			return new VendirSyncResult(success, success ? null : "Vendir exited with code " + exitCode, output);

		}
		catch (Exception e) {
			logger.error("Failed to execute vendir sync", e);
			return new VendirSyncResult(false, e.getMessage(), "");
		}
	}

	private void deleteRecursively(Path path) throws IOException {
		if (Files.exists(path)) {
			Files.walk(path)
				.sorted((a, b) -> b.compareTo(a)) // Reverse order for deletion
				.forEach(p -> {
					try {
						Files.delete(p);
					}
					catch (IOException e) {
						logger.warn("Failed to delete: {}", p, e);
					}
				});
		}
	}

	@Override
	public String getName() {
		return "VendirContext";
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Creates a new builder for VendirContextAdvisor.
	 * @return a new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for VendirContextAdvisor.
	 */
	public static class Builder {

		private Path vendirConfigPath;

		private String contextDirectory = ".agent-context/vendir";

		private boolean autoCleanup = false;

		private long timeoutSeconds = 300; // 5 minutes default

		private int order = Ordered.HIGHEST_PRECEDENCE + 100;

		/**
		 * Sets the path to vendir.yml configuration file.
		 * @param vendirConfigPath path to vendir configuration
		 * @return this builder
		 */
		public Builder vendirConfigPath(Path vendirConfigPath) {
			this.vendirConfigPath = vendirConfigPath;
			return this;
		}

		/**
		 * Sets the path to vendir.yml configuration file.
		 * @param vendirConfigPath path string to vendir configuration
		 * @return this builder
		 */
		public Builder vendirConfigPath(String vendirConfigPath) {
			this.vendirConfigPath = Path.of(vendirConfigPath);
			return this;
		}

		/**
		 * Sets the context directory relative to agent working directory.
		 * @param contextDirectory context directory path (default:
		 * ".agent-context/vendir")
		 * @return this builder
		 */
		public Builder contextDirectory(String contextDirectory) {
			this.contextDirectory = contextDirectory;
			return this;
		}

		/**
		 * Enables or disables automatic cleanup of context files after agent execution.
		 * @param autoCleanup true to clean up context files (default: false)
		 * @return this builder
		 */
		public Builder autoCleanup(boolean autoCleanup) {
			this.autoCleanup = autoCleanup;
			return this;
		}

		/**
		 * Sets the timeout for vendir sync operation.
		 * @param timeoutSeconds timeout in seconds (default: 300)
		 * @return this builder
		 */
		public Builder timeout(long timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
			return this;
		}

		/**
		 * Sets the advisor execution order.
		 * @param order Spring Ordered value (default: HIGHEST_PRECEDENCE + 100)
		 * @return this builder
		 */
		public Builder order(int order) {
			this.order = order;
			return this;
		}

		/**
		 * Builds the VendirContextAdvisor.
		 * @return a new VendirContextAdvisor instance
		 * @throws IllegalStateException if vendirConfigPath is not set
		 */
		public VendirContextAdvisor build() {
			if (this.vendirConfigPath == null) {
				throw new IllegalStateException("vendirConfigPath must be set");
			}
			return new VendirContextAdvisor(this.vendirConfigPath, this.contextDirectory, this.autoCleanup,
					this.timeoutSeconds, this.order);
		}

	}

	/**
	 * Result of vendir sync operation.
	 */
	private static class VendirSyncResult {

		final boolean success;

		final String error;

		final String output;

		VendirSyncResult(boolean success, String error, String output) {
			this.success = success;
			this.error = error;
			this.output = output;
		}

	}

}
