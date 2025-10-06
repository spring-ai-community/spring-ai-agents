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
import java.util.Arrays;
import java.util.List;

/**
 * Fluent builder for creating VendirContextAdvisor instances from Git repository
 * configurations.
 *
 * <p>
 * This builder provides a high-level DSL for specifying Git repositories to fetch as
 * context materials, hiding the complexity of vendir.yml configuration.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Single repository
 * VendirContextAdvisor advisor = GitRepoContextBuilder
 *     .from("https://github.com/spring-guides/gs-rest-service")
 *     .subdirectory("complete")
 *     .build();
 *
 * // Multiple repositories with JaCoCo reference
 * VendirContextAdvisor advisor = GitRepoContextBuilder
 *     .repos(
 *         GitRepo.from("https://github.com/spring-guides/gs-rest-service")
 *                .ref("main")
 *                .subdirectory("complete")
 *                .as("target-project"),
 *         GitRepo.from("https://github.com/jacoco/jacoco")
 *                .subdirectory("org.jacoco.doc")
 *                .as("jacoco-reference")
 *     )
 *     .build();
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class GitRepoContextBuilder {

	private final List<GitRepo> repositories = new ArrayList<>();

	private String contextPath = ".agent-context/git";

	private boolean autoCleanup = false;

	private long timeoutSeconds = 300;

	private GitRepoContextBuilder() {
	}

	/**
	 * Create a builder for a single Git repository.
	 * @param url the Git repository URL
	 * @return a new builder instance
	 */
	public static GitRepoBuilder from(String url) {
		return new GitRepoBuilder(url);
	}

	/**
	 * Create a builder for multiple Git repositories.
	 * @param repos the Git repositories to fetch
	 * @return a new builder instance
	 */
	public static GitRepoContextBuilder repos(GitRepo... repos) {
		GitRepoContextBuilder builder = new GitRepoContextBuilder();
		builder.repositories.addAll(Arrays.asList(repos));
		return builder;
	}

	/**
	 * Set the context directory relative to agent working directory.
	 * @param contextPath context directory path (default: ".agent-context/git")
	 * @return this builder for chaining
	 */
	public GitRepoContextBuilder contextPath(String contextPath) {
		this.contextPath = contextPath;
		return this;
	}

	/**
	 * Enable automatic cleanup of context files after agent execution.
	 * @param autoCleanup true to clean up context files (default: false)
	 * @return this builder for chaining
	 */
	public GitRepoContextBuilder autoCleanup(boolean autoCleanup) {
		this.autoCleanup = autoCleanup;
		return this;
	}

	/**
	 * Set the timeout for vendir sync operation.
	 * @param timeoutSeconds timeout in seconds (default: 300)
	 * @return this builder for chaining
	 */
	public GitRepoContextBuilder timeout(long timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
		return this;
	}

	/**
	 * Build the VendirContextAdvisor.
	 * @return a configured VendirContextAdvisor
	 */
	public VendirContextAdvisor build() {
		if (repositories.isEmpty()) {
			throw new IllegalStateException("At least one Git repository must be specified");
		}

		try {
			// Generate vendir.yml content
			String vendirYml = generateVendirYml();

			// Create temporary vendir.yml file
			Path tempVendirYml = Files.createTempFile("vendir-", ".yml");
			Files.writeString(tempVendirYml, vendirYml);

			// Build VendirContextAdvisor
			return VendirContextAdvisor.builder()
				.vendirConfigPath(tempVendirYml)
				.contextDirectory(contextPath)
				.autoCleanup(autoCleanup)
				.timeout(timeoutSeconds)
				.build();

		}
		catch (IOException e) {
			throw new RuntimeException("Failed to create vendir configuration", e);
		}
	}

	private String generateVendirYml() {
		StringBuilder yml = new StringBuilder();
		yml.append("apiVersion: vendir.k14s.io/v1alpha1\n");
		yml.append("kind: Config\n");
		yml.append("directories:\n");
		yml.append("- path: vendor\n");
		yml.append("  contents:\n");

		for (GitRepo repo : repositories) {
			yml.append("  - path: ").append(repo.getAlias()).append("\n");
			yml.append("    git:\n");
			yml.append("      url: ").append(repo.getUrl()).append("\n");
			yml.append("      ref: ").append(repo.getRef()).append("\n");
			yml.append("      depth: ").append(repo.getDepth()).append("\n");

			// Add subdirectory extraction if specified
			if (repo.getSubdirectory() != null && !repo.getSubdirectory().isBlank()) {
				yml.append("    includePaths:\n");
				yml.append("    - ").append(repo.getSubdirectory()).append("/**/*\n");
				yml.append("    newRootPath: ").append(repo.getSubdirectory()).append("\n");
			}
		}

		return yml.toString();
	}

	/**
	 * Builder for single Git repository configuration.
	 */
	public static class GitRepoBuilder {

		private final GitRepo repo;

		private String contextPath = ".agent-context/git";

		private boolean autoCleanup = false;

		private long timeoutSeconds = 300;

		private GitRepoBuilder(String url) {
			this.repo = GitRepo.from(url);
		}

		/**
		 * Set the Git ref (branch, tag, or commit).
		 * @param ref the ref to checkout
		 * @return this builder for chaining
		 */
		public GitRepoBuilder ref(String ref) {
			repo.ref(ref);
			return this;
		}

		/**
		 * Set subdirectory to extract as root.
		 * @param subdirectory the subdirectory path
		 * @return this builder for chaining
		 */
		public GitRepoBuilder subdirectory(String subdirectory) {
			repo.subdirectory(subdirectory);
			return this;
		}

		/**
		 * Set clone depth for shallow clones.
		 * @param depth the depth
		 * @return this builder for chaining
		 */
		public GitRepoBuilder depth(int depth) {
			repo.depth(depth);
			return this;
		}

		/**
		 * Set alias name for this repository.
		 * @param alias the alias name
		 * @return this builder for chaining
		 */
		public GitRepoBuilder as(String alias) {
			repo.as(alias);
			return this;
		}

		/**
		 * Set the context directory.
		 * @param contextPath context directory path
		 * @return this builder for chaining
		 */
		public GitRepoBuilder contextPath(String contextPath) {
			this.contextPath = contextPath;
			return this;
		}

		/**
		 * Enable automatic cleanup.
		 * @param autoCleanup true to clean up
		 * @return this builder for chaining
		 */
		public GitRepoBuilder autoCleanup(boolean autoCleanup) {
			this.autoCleanup = autoCleanup;
			return this;
		}

		/**
		 * Set the timeout.
		 * @param timeoutSeconds timeout in seconds
		 * @return this builder for chaining
		 */
		public GitRepoBuilder timeout(long timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
			return this;
		}

		/**
		 * Build the VendirContextAdvisor.
		 * @return a configured VendirContextAdvisor
		 */
		public VendirContextAdvisor build() {
			return GitRepoContextBuilder.repos(repo)
				.contextPath(contextPath)
				.autoCleanup(autoCleanup)
				.timeout(timeoutSeconds)
				.build();
		}

	}

}
