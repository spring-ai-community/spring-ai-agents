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

/**
 * Configuration for a Git repository to be fetched via vendir.
 *
 * <p>
 * Provides a fluent API for specifying Git repository details including URL, ref,
 * subdirectory extraction, and depth for shallow clones.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * GitRepo repo = GitRepo.from("https://github.com/spring-guides/gs-rest-service")
 *     .ref("main")
 *     .subdirectory("complete")  // Extract 'complete' as root
 *     .depth(1)                  // Shallow clone
 *     .as("target-project");
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class GitRepo {

	private final String url;

	private String ref = "main";

	private String subdirectory;

	private int depth = 1;

	private String alias;

	private GitRepo(String url) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("Git URL cannot be null or empty");
		}
		this.url = url;
	}

	/**
	 * Create a GitRepo configuration from a URL.
	 * @param url the Git repository URL
	 * @return a new GitRepo builder
	 */
	public static GitRepo from(String url) {
		return new GitRepo(url);
	}

	/**
	 * Set the Git ref (branch, tag, or commit).
	 * @param ref the ref to checkout (default: main)
	 * @return this GitRepo for chaining
	 */
	public GitRepo ref(String ref) {
		this.ref = ref;
		return this;
	}

	/**
	 * Set subdirectory to extract as root.
	 * <p>
	 * When specified, vendir will extract only this subdirectory and place its contents
	 * at the root of the destination directory.
	 * </p>
	 * @param subdirectory the subdirectory path (e.g., "complete")
	 * @return this GitRepo for chaining
	 */
	public GitRepo subdirectory(String subdirectory) {
		this.subdirectory = subdirectory;
		return this;
	}

	/**
	 * Set clone depth for shallow clones.
	 * @param depth the depth (default: 1)
	 * @return this GitRepo for chaining
	 */
	public GitRepo depth(int depth) {
		this.depth = depth;
		return this;
	}

	/**
	 * Set alias name for this repository in the vendor directory.
	 * @param alias the alias name (e.g., "target-project")
	 * @return this GitRepo for chaining
	 */
	public GitRepo as(String alias) {
		this.alias = alias;
		return this;
	}

	// Getters
	public String getUrl() {
		return url;
	}

	public String getRef() {
		return ref;
	}

	public String getSubdirectory() {
		return subdirectory;
	}

	public int getDepth() {
		return depth;
	}

	public String getAlias() {
		return alias != null ? alias : deriveAliasFromUrl();
	}

	private String deriveAliasFromUrl() {
		// Extract repo name from URL
		// e.g., "https://github.com/spring-guides/gs-rest-service" -> "gs-rest-service"
		String[] parts = url.split("/");
		String repoName = parts[parts.length - 1];
		if (repoName.endsWith(".git")) {
			repoName = repoName.substring(0, repoName.length() - 4);
		}
		return repoName;
	}

}
