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

package org.springaicommunity.agents.geminisdk.transport;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

/**
 * Configuration options for Gemini CLI commands in autonomous (yolo) mode.
 *
 * <p>
 * This class provides a comprehensive mapping of Gemini CLI flags that are compatible
 * with autonomous operation. It focuses on options that work well with programmatic usage
 * and the {@code --yolo} mode for automatic action approval.
 *
 * <p>
 * The class follows an immutable design pattern using Java records and provides builder
 * methods for fluent configuration. All options include validation and sensible defaults
 * for production use.
 *
 * @see <a href="https://github.com/google-gemini/gemini-cli">Gemini CLI Documentation</a>
 */
public record CLIOptions(
		/**
		 * The Gemini model to use for processing.
		 *
		 * <p>
		 * Corresponds to the {@code -m, --model} CLI flag. Supports model names like:
		 * <ul>
		 * <li>{@code gemini-2.5-pro} - High-quality model for complex tasks</li>
		 * <li>{@code gemini-2.5-flash} - Fast model for quick responses</li>
		 * <li>{@code models/gemini-2.5-pro} - Fully qualified model names</li>
		 * </ul>
		 *
		 * @see #validateModelName(String) for validation rules
		 */
		String model,

		/**
		 * Whether to automatically accept all actions (YOLO mode).
		 *
		 * <p>
		 * Corresponds to the {@code -y, --yolo} CLI flag. When {@code true}, the CLI will
		 * automatically approve all tool executions and file modifications without
		 * prompting for user confirmation. This is essential for autonomous operation.
		 *
		 * <p>
		 * Defaults to {@code true} for programmatic usage.
		 */
		boolean yoloMode,

		/**
		 * Whether to include all files in the current directory context.
		 *
		 * <p>
		 * Corresponds to the {@code -a, --all-files} CLI flag. When {@code true}, the CLI
		 * will include all files in the working directory in the context provided to the
		 * model. Use with caution as this can significantly increase token usage and
		 * processing time.
		 *
		 * <p>
		 * Defaults to {@code false} for controlled context management.
		 */
		boolean allFiles,

		/**
		 * Maximum time to wait for CLI command completion.
		 *
		 * <p>
		 * This is an SDK-specific timeout, not a CLI flag. It controls how long the Java
		 * process will wait for the Gemini CLI to respond before timing out. The timeout
		 * includes both model processing time and network latency.
		 *
		 * <p>
		 * Defaults to 2 minutes. Maximum allowed is 30 minutes.
		 */
		Duration timeout,

		/**
		 * Whether to run in debug mode for enhanced logging.
		 *
		 * <p>
		 * Corresponds to the {@code -d, --debug} CLI flag. When {@code true}, the CLI
		 * will output detailed debug information including request/response details,
		 * timing information, and internal processing steps.
		 *
		 * <p>
		 * Useful for troubleshooting but may impact performance. Defaults to
		 * {@code false}.
		 */
		boolean debug,

		/**
		 * Whether to run the CLI in a sandboxed environment.
		 *
		 * <p>
		 * Corresponds to the {@code -s, --sandbox} CLI flag. When {@code true}, the CLI
		 * will execute in an isolated environment for enhanced security. This is
		 * particularly useful when processing untrusted code or data.
		 *
		 * <p>
		 * Defaults to {@code false}. May require additional setup depending on
		 * environment.
		 */
		boolean sandbox,

		/**
		 * Custom sandbox image URI for containerized execution.
		 *
		 * <p>
		 * Corresponds to the {@code --sandbox-image} CLI flag. Specifies a custom
		 * container image to use when {@link #sandbox} is enabled. The image should
		 * contain the necessary runtime environment for code execution.
		 *
		 * <p>
		 * Only used when {@link #sandbox} is {@code true}. Defaults to {@code null} to
		 * use the CLI's default sandbox image.
		 */
		String sandboxImage,

		/**
		 * Additional directories to include in the workspace context.
		 *
		 * <p>
		 * Corresponds to the {@code --include-directories} CLI flag. These directories
		 * will be made available to the model in addition to the current working
		 * directory. Useful for providing broader context or accessing shared libraries.
		 *
		 * <p>
		 * Each directory path should be absolute or relative to the working directory.
		 * Defaults to an empty set.
		 */
		Set<String> includeDirectories,

		/**
		 * Specific extensions to enable for this session.
		 *
		 * <p>
		 * Corresponds to the {@code -e, --extensions} CLI flag. When specified, only the
		 * listed extensions will be available. If empty or null, all available extensions
		 * are used by default.
		 *
		 * <p>
		 * Extensions provide additional capabilities like language-specific tools,
		 * integrations, or specialized processing modes.
		 */
		Set<String> extensions,

		/**
		 * Proxy configuration for network requests.
		 *
		 * <p>
		 * Corresponds to the {@code --proxy} CLI flag. Specifies a proxy server for all
		 * network communication in the format: {@code schema://user:password@host:port}
		 *
		 * <p>
		 * Examples:
		 * <ul>
		 * <li>{@code http://proxy.company.com:8080}</li>
		 * <li>{@code https://user:pass@secure-proxy.com:443}</li>
		 * <li>{@code socks5://localhost:1080}</li>
		 * </ul>
		 *
		 * <p>
		 * Defaults to {@code null} for direct connections.
		 */
		String proxy) {

	public CLIOptions {
		// Apply defaults for null values
		if (timeout == null) {
			timeout = Duration.ofMinutes(2);
		}
		if (includeDirectories == null) {
			includeDirectories = Collections.emptySet();
		}
		if (extensions == null) {
			extensions = Collections.emptySet();
		}

		// Validate timeout is reasonable
		if (timeout.isNegative()) {
			throw new IllegalArgumentException("Timeout cannot be negative");
		}
		if (timeout.toMinutes() > 30) {
			throw new IllegalArgumentException("Timeout cannot exceed 30 minutes");
		}

		// Validate model name if provided
		if (model != null) {
			String trimmedModel = model.trim();
			if (trimmedModel.isEmpty()) {
				throw new IllegalArgumentException("Model name cannot be empty or whitespace");
			}
			validateModelName(trimmedModel);
		}

		// Validate sandbox configuration
		if (sandboxImage != null && !sandbox) {
			throw new IllegalArgumentException("sandboxImage can only be specified when sandbox is true");
		}

		// Validate proxy format if provided
		if (proxy != null) {
			validateProxyFormat(proxy.trim());
		}
	}

	private static void validateModelName(String model) {
		// Validate against known Gemini model patterns
		List<String> validPrefixes = List.of("gemini-", "models/gemini-");
		boolean isValid = validPrefixes.stream().anyMatch(model::startsWith) || model.equals("gemini-pro")
				|| model.equals("gemini-flash") || model.equals("gemini-2.5-flash");

		if (!isValid) {
			throw new IllegalArgumentException(
					"Invalid model name: " + model + ". Expected format: 'gemini-*' or 'models/gemini-*'");
		}
	}

	private static void validateProxyFormat(String proxy) {
		if (proxy.isEmpty()) {
			throw new IllegalArgumentException("Proxy cannot be empty or whitespace");
		}

		// Basic validation for proxy URL format
		if (!proxy.contains("://")) {
			throw new IllegalArgumentException(
					"Invalid proxy format: " + proxy + ". Expected format: schema://[user:password@]host:port");
		}

		String scheme = proxy.substring(0, proxy.indexOf("://")).toLowerCase();
		List<String> validSchemes = List.of("http", "https", "socks4", "socks5");
		if (!validSchemes.contains(scheme)) {
			throw new IllegalArgumentException(
					"Invalid proxy scheme: " + scheme + ". Supported schemes: " + validSchemes);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a CLIOptions instance with sensible defaults for autonomous operation.
	 * @return CLIOptions with yolo mode enabled and 2-minute timeout
	 */
	public static CLIOptions defaultOptions() {
		return new CLIOptions(null, true, false, Duration.ofMinutes(2), false, false, null, Collections.emptySet(),
				Collections.emptySet(), null);
	}

	// Convenience getters
	public Duration getTimeout() {
		return timeout;
	}

	public String getModel() {
		return model;
	}

	public boolean isYoloMode() {
		return yoloMode;
	}

	public boolean isAllFiles() {
		return allFiles;
	}

	public boolean isDebug() {
		return debug;
	}

	public boolean isSandbox() {
		return sandbox;
	}

	public String getSandboxImage() {
		return sandboxImage;
	}

	public Set<String> getIncludeDirectories() {
		return includeDirectories;
	}

	public Set<String> getExtensions() {
		return extensions;
	}

	public String getProxy() {
		return proxy;
	}

	public static class Builder {

		private String model;

		private boolean yoloMode = true; // Default to true for programmatic use

		private boolean allFiles = false;

		private Duration timeout = Duration.ofMinutes(2);

		private boolean debug = false;

		private boolean sandbox = false;

		private String sandboxImage;

		private Set<String> includeDirectories = Collections.emptySet();

		private Set<String> extensions = Collections.emptySet();

		private String proxy;

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder yoloMode(boolean yoloMode) {
			this.yoloMode = yoloMode;
			return this;
		}

		public Builder allFiles(boolean allFiles) {
			this.allFiles = allFiles;
			return this;
		}

		public Builder timeout(Duration timeout) {
			if (timeout != null && timeout.isNegative()) {
				throw new IllegalArgumentException("Timeout cannot be negative");
			}
			this.timeout = timeout;
			return this;
		}

		public Builder timeoutSeconds(int seconds) {
			return timeout(Duration.ofSeconds(seconds));
		}

		public Builder timeoutMinutes(int minutes) {
			return timeout(Duration.ofMinutes(minutes));
		}

		/**
		 * Enables debug mode for enhanced logging and troubleshooting.
		 * @param debug whether to enable debug mode
		 * @return this builder instance
		 */
		public Builder debug(boolean debug) {
			this.debug = debug;
			return this;
		}

		/**
		 * Enables sandboxed execution for enhanced security.
		 * @param sandbox whether to enable sandbox mode
		 * @return this builder instance
		 */
		public Builder sandbox(boolean sandbox) {
			this.sandbox = sandbox;
			return this;
		}

		/**
		 * Sets a custom sandbox image URI for containerized execution.
		 * @param sandboxImage the sandbox image URI
		 * @return this builder instance
		 */
		public Builder sandboxImage(String sandboxImage) {
			this.sandboxImage = sandboxImage;
			return this;
		}

		/**
		 * Sets additional directories to include in the workspace context.
		 * @param includeDirectories set of directory paths to include
		 * @return this builder instance
		 */
		public Builder includeDirectories(Set<String> includeDirectories) {
			this.includeDirectories = includeDirectories != null ? includeDirectories : Collections.emptySet();
			return this;
		}

		/**
		 * Adds a single directory to the workspace context.
		 * @param directory the directory path to include
		 * @return this builder instance
		 */
		public Builder addIncludeDirectory(String directory) {
			if (directory != null) {
				if (this.includeDirectories.isEmpty()) {
					this.includeDirectories = Collections.singleton(directory);
				}
				else {
					Set<String> newDirs = new java.util.HashSet<>(this.includeDirectories);
					newDirs.add(directory);
					this.includeDirectories = newDirs;
				}
			}
			return this;
		}

		/**
		 * Sets specific extensions to enable for this session.
		 * @param extensions set of extension names to enable
		 * @return this builder instance
		 */
		public Builder extensions(Set<String> extensions) {
			this.extensions = extensions != null ? extensions : Collections.emptySet();
			return this;
		}

		/**
		 * Adds a single extension to be enabled for this session.
		 * @param extension the extension name to enable
		 * @return this builder instance
		 */
		public Builder addExtension(String extension) {
			if (extension != null) {
				if (this.extensions.isEmpty()) {
					this.extensions = Collections.singleton(extension);
				}
				else {
					Set<String> newExts = new java.util.HashSet<>(this.extensions);
					newExts.add(extension);
					this.extensions = newExts;
				}
			}
			return this;
		}

		/**
		 * Sets proxy configuration for network requests.
		 * @param proxy the proxy configuration string
		 * @return this builder instance
		 */
		public Builder proxy(String proxy) {
			this.proxy = proxy;
			return this;
		}

		public CLIOptions build() {
			return new CLIOptions(model, yoloMode, allFiles, timeout, debug, sandbox, sandboxImage, includeDirectories,
					extensions, proxy);
		}

	}

	// Convenience factory methods for common configurations

	/**
	 * Creates CLIOptions optimized for fast responses with minimal latency. Uses the
	 * fastest model with a short timeout.
	 * @return CLIOptions configured for speed
	 */
	public static CLIOptions fastResponse() {
		return builder().model("gemini-2.5-flash").yoloMode(true).timeout(Duration.ofSeconds(30)).build();
	}

	/**
	 * Creates CLIOptions optimized for high-quality, complex responses. Uses the most
	 * capable model with extended timeout.
	 * @return CLIOptions configured for quality
	 */
	public static CLIOptions highQuality() {
		return builder().model("gemini-2.5-pro").yoloMode(true).timeout(Duration.ofMinutes(5)).build();
	}

	/**
	 * Creates CLIOptions optimized for development and testing scenarios. Uses fast model
	 * with debug logging enabled.
	 * @return CLIOptions configured for development
	 */
	public static CLIOptions development() {
		return builder().model("gemini-2.5-flash")
			.yoloMode(true)
			.allFiles(false)
			.debug(true)
			.timeout(Duration.ofMinutes(1))
			.build();
	}

	/**
	 * Creates CLIOptions with sandboxing enabled for secure execution. Suitable for
	 * processing untrusted code or data.
	 * @return CLIOptions configured for secure sandboxed execution
	 */
	public static CLIOptions sandboxed() {
		return builder().model("gemini-2.5-flash").yoloMode(true).sandbox(true).timeout(Duration.ofMinutes(3)).build();
	}
}