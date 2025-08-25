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

package org.springaicommunity.agents.sweagentsdk.types;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive configuration options for mini-SWE-agent CLI commands.
 *
 * <p>
 * This class provides a complete mapping of mini-SWE-agent CLI flags and configuration
 * options compatible with autonomous operation. It focuses on options that work well with
 * programmatic usage and includes both CLI flags and agent configuration.
 *
 * <p>
 * The class follows an immutable design pattern using the builder pattern and provides
 * validation and sensible defaults for production use.
 *
 * @author Mark Pollack
 * @since 1.1.0
 * @see <a href="https://mini-swe-agent.com/latest/">mini-SWE-agent Documentation</a>
 */
public class SweAgentOptions {

	/**
	 * The model name to use for the underlying LLM.
	 *
	 * <p>
	 * Corresponds to the {@code -m, --model} CLI flag. Supports model names like:
	 * <ul>
	 * <li>{@code gpt-4o} - OpenAI GPT-4 Omni</li>
	 * <li>{@code gpt-4o-mini} - OpenAI GPT-4 Omni Mini</li>
	 * <li>{@code claude-3-5-sonnet} - Anthropic Claude 3.5 Sonnet</li>
	 * <li>{@code gpt-3.5-turbo} - OpenAI GPT-3.5 Turbo</li>
	 * </ul>
	 *
	 * Defaults to model specified in configuration file or environment.
	 */
	private String model;

	/**
	 * Whether to automatically accept all actions (YOLO mode).
	 *
	 * <p>
	 * Corresponds to the {@code -y, --yolo} CLI flag. When {@code true}, the CLI will
	 * automatically approve all bash commands and file modifications without prompting
	 * for user confirmation. This is essential for autonomous operation.
	 *
	 * <p>
	 * Defaults to {@code true} for programmatic usage.
	 */
	private boolean yoloMode = true;

	/**
	 * Whether to exit immediately when the agent wants to finish.
	 *
	 * <p>
	 * Corresponds to the {@code --exit-immediately} CLI flag. When {@code true}, the
	 * agent will exit immediately when it completes the task instead of prompting for
	 * confirmation. This is useful for autonomous operation.
	 *
	 * <p>
	 * Defaults to {@code true} for programmatic usage.
	 */
	private boolean exitImmediately = true;

	/**
	 * Maximum cost limit for the task execution in USD.
	 *
	 * <p>
	 * Corresponds to the {@code -l, --cost-limit} CLI flag. When set, the agent will stop
	 * execution if the estimated cost exceeds this limit. Set to 0 to disable cost
	 * limiting.
	 *
	 * <p>
	 * Defaults to {@code null} (no cost limit).
	 */
	private Double costLimit;

	/**
	 * Path to the configuration file to use.
	 *
	 * <p>
	 * Corresponds to the {@code -c, --config} CLI flag. Specifies a custom configuration
	 * file instead of the default. The configuration file contains agent behavior
	 * settings, prompts, and environment configuration.
	 *
	 * <p>
	 * Defaults to {@code null} to use the built-in configuration.
	 */
	private Path configPath;

	/**
	 * Path to save the trajectory file.
	 *
	 * <p>
	 * Corresponds to the {@code -o, --output} CLI flag. The trajectory file contains the
	 * complete execution history including all commands and outputs. Useful for debugging
	 * and analysis.
	 *
	 * <p>
	 * Defaults to {@code null} to use default trajectory location.
	 */
	private Path outputPath;

	/**
	 * Maximum time to wait for CLI command completion.
	 *
	 * <p>
	 * This is an SDK-specific timeout, not a CLI flag. It controls how long the Java
	 * process will wait for the mini-SWE-agent CLI to respond before timing out. The
	 * timeout includes both model processing time and command execution.
	 *
	 * <p>
	 * Defaults to 5 minutes. Maximum recommended is 30 minutes.
	 */
	private Duration timeout = Duration.ofMinutes(5);

	/**
	 * Working directory for agent execution.
	 *
	 * <p>
	 * This is the directory where the mini-SWE-agent will execute commands and look for
	 * files. If null, the working directory will be passed explicitly to the execute
	 * method.
	 *
	 * <p>
	 * Defaults to {@code null}.
	 */
	private String workingDirectory;

	/**
	 * Additional environment variables to set for the agent process.
	 *
	 * <p>
	 * These environment variables will be passed to the mini-SWE-agent CLI process.
	 * Common variables include API keys, proxy settings, and tool configurations.
	 *
	 * <p>
	 * Defaults to empty map.
	 */
	private Map<String, String> environmentVariables = Collections.emptyMap();

	/**
	 * Path to the mini-SWE-agent CLI executable.
	 *
	 * <p>
	 * If specified, this exact path will be used instead of the automatic discovery
	 * process. Useful when the CLI is installed in a non-standard location or when using
	 * a custom build.
	 *
	 * <p>
	 * Defaults to {@code null} to use automatic discovery.
	 */
	private String executablePath;

	/**
	 * Maximum number of agent iterations before stopping.
	 *
	 * <p>
	 * This controls how many action/observation cycles the agent can perform. Each
	 * iteration involves the agent taking an action (running a command) and receiving
	 * feedback. Higher values allow for more complex tasks but may increase cost and
	 * execution time.
	 *
	 * <p>
	 * Corresponds to the agent's step_limit configuration. Defaults to 20. Set to 0 for
	 * unlimited iterations (not recommended).
	 */
	private int maxIterations = 20;

	/**
	 * Whether to enable debug/verbose output.
	 *
	 * <p>
	 * When enabled, provides detailed logging of the agent's decision-making process,
	 * command execution, and internal state. Useful for troubleshooting and understanding
	 * agent behavior.
	 *
	 * <p>
	 * Defaults to {@code false} for clean output.
	 */
	private boolean verbose = false;

	/**
	 * Whether to use visual/pager-style UI.
	 *
	 * <p>
	 * Corresponds to the {@code -v, --visual} CLI flag. When {@code true}, uses the
	 * Textual-based pager interface instead of the simple REPL interface. Generally
	 * should be {@code false} for programmatic usage.
	 *
	 * <p>
	 * Defaults to {@code false} for programmatic usage.
	 */
	private boolean visual = false;

	/**
	 * Custom system prompt template for the agent.
	 *
	 * <p>
	 * When specified, overrides the default system prompt in the configuration. The
	 * system prompt defines the agent's behavior, capabilities, and response format. Use
	 * with caution as incorrect prompts can break agent functionality.
	 *
	 * <p>
	 * Defaults to {@code null} to use configuration default.
	 */
	private String systemPrompt;

	/**
	 * Model-specific parameters.
	 *
	 * <p>
	 * Additional parameters to pass to the underlying language model, such as
	 * temperature, top_p, max_tokens, etc. The exact parameters supported depend on the
	 * model being used.
	 *
	 * <p>
	 * Example: {@code {"temperature": "0.0", "max_tokens": "4000"}}
	 *
	 * <p>
	 * Defaults to empty map to use model defaults.
	 */
	private Map<String, Object> modelParameters = Collections.emptyMap();

	/**
	 * Additional CLI arguments to pass to mini-SWE-agent.
	 *
	 * <p>
	 * For advanced use cases where you need to pass specific CLI arguments not covered by
	 * the typed options. Use with caution as invalid arguments will cause execution to
	 * fail.
	 *
	 * <p>
	 * Defaults to empty set.
	 */
	private Set<String> additionalArgs = Collections.emptySet();

	/**
	 * Default constructor with sensible defaults for autonomous operation.
	 */
	public SweAgentOptions() {
	}

	/**
	 * Constructor with model specification.
	 * @param model the model name to use
	 */
	public SweAgentOptions(String model) {
		this.model = model;
	}

	// Getters and setters

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public boolean isYoloMode() {
		return yoloMode;
	}

	public void setYoloMode(boolean yoloMode) {
		this.yoloMode = yoloMode;
	}

	public boolean isExitImmediately() {
		return exitImmediately;
	}

	public void setExitImmediately(boolean exitImmediately) {
		this.exitImmediately = exitImmediately;
	}

	public Double getCostLimit() {
		return costLimit;
	}

	public void setCostLimit(Double costLimit) {
		this.costLimit = costLimit;
	}

	public Path getConfigPath() {
		return configPath;
	}

	public void setConfigPath(Path configPath) {
		this.configPath = configPath;
	}

	public Path getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(Path outputPath) {
		this.outputPath = outputPath;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(Map<String, String> environmentVariables) {
		this.environmentVariables = environmentVariables != null ? environmentVariables : Collections.emptyMap();
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isVisual() {
		return visual;
	}

	public void setVisual(boolean visual) {
		this.visual = visual;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public Map<String, Object> getModelParameters() {
		return modelParameters;
	}

	public void setModelParameters(Map<String, Object> modelParameters) {
		this.modelParameters = modelParameters != null ? modelParameters : Collections.emptyMap();
	}

	public Set<String> getAdditionalArgs() {
		return additionalArgs;
	}

	public void setAdditionalArgs(Set<String> additionalArgs) {
		this.additionalArgs = additionalArgs != null ? additionalArgs : Collections.emptySet();
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating SweAgentOptions with a fluent interface.
	 */
	public static final class Builder {

		private final SweAgentOptions options = new SweAgentOptions();

		private Builder() {
		}

		public Builder model(String model) {
			options.setModel(model);
			return this;
		}

		public Builder yoloMode(boolean yoloMode) {
			options.setYoloMode(yoloMode);
			return this;
		}

		public Builder exitImmediately(boolean exitImmediately) {
			options.setExitImmediately(exitImmediately);
			return this;
		}

		public Builder costLimit(Double costLimit) {
			options.setCostLimit(costLimit);
			return this;
		}

		public Builder configPath(Path configPath) {
			options.setConfigPath(configPath);
			return this;
		}

		public Builder outputPath(Path outputPath) {
			options.setOutputPath(outputPath);
			return this;
		}

		public Builder timeout(Duration timeout) {
			if (timeout != null && timeout.isNegative()) {
				throw new IllegalArgumentException("Timeout cannot be negative");
			}
			options.setTimeout(timeout);
			return this;
		}

		/**
		 * Sets timeout in minutes for convenience.
		 * @param minutes timeout in minutes
		 * @return this builder instance
		 */
		public Builder timeoutMinutes(int minutes) {
			return timeout(Duration.ofMinutes(minutes));
		}

		/**
		 * Sets timeout in seconds for convenience.
		 * @param seconds timeout in seconds
		 * @return this builder instance
		 */
		public Builder timeoutSeconds(int seconds) {
			return timeout(Duration.ofSeconds(seconds));
		}

		public Builder workingDirectory(String workingDirectory) {
			options.setWorkingDirectory(workingDirectory);
			return this;
		}

		public Builder environmentVariables(Map<String, String> environmentVariables) {
			options.setEnvironmentVariables(environmentVariables);
			return this;
		}

		/**
		 * Adds a single environment variable.
		 * @param key the environment variable name
		 * @param value the environment variable value
		 * @return this builder instance
		 */
		public Builder addEnvironmentVariable(String key, String value) {
			if (key != null && value != null) {
				Map<String, String> currentEnv = options.getEnvironmentVariables();
				Map<String, String> newEnv = new java.util.HashMap<>(currentEnv);
				newEnv.put(key, value);
				options.setEnvironmentVariables(newEnv);
			}
			return this;
		}

		public Builder executablePath(String executablePath) {
			options.setExecutablePath(executablePath);
			return this;
		}

		public Builder maxIterations(int maxIterations) {
			if (maxIterations < 0) {
				throw new IllegalArgumentException("maxIterations cannot be negative");
			}
			options.setMaxIterations(maxIterations);
			return this;
		}

		public Builder verbose(boolean verbose) {
			options.setVerbose(verbose);
			return this;
		}

		public Builder visual(boolean visual) {
			options.setVisual(visual);
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			options.setSystemPrompt(systemPrompt);
			return this;
		}

		public Builder modelParameters(Map<String, Object> modelParameters) {
			options.setModelParameters(modelParameters);
			return this;
		}

		/**
		 * Adds a single model parameter.
		 * @param key the parameter name
		 * @param value the parameter value
		 * @return this builder instance
		 */
		public Builder addModelParameter(String key, Object value) {
			if (key != null && value != null) {
				Map<String, Object> currentParams = options.getModelParameters();
				Map<String, Object> newParams = new java.util.HashMap<>(currentParams);
				newParams.put(key, value);
				options.setModelParameters(newParams);
			}
			return this;
		}

		public Builder additionalArgs(Set<String> additionalArgs) {
			options.setAdditionalArgs(additionalArgs);
			return this;
		}

		/**
		 * Adds a single additional CLI argument.
		 * @param arg the CLI argument to add
		 * @return this builder instance
		 */
		public Builder addAdditionalArg(String arg) {
			if (arg != null) {
				Set<String> currentArgs = options.getAdditionalArgs();
				Set<String> newArgs = new java.util.HashSet<>(currentArgs);
				newArgs.add(arg);
				options.setAdditionalArgs(newArgs);
			}
			return this;
		}

		public SweAgentOptions build() {
			// Validation
			if (options.getTimeout() != null && options.getTimeout().isNegative()) {
				throw new IllegalArgumentException("Timeout cannot be negative");
			}
			if (options.getMaxIterations() < 0) {
				throw new IllegalArgumentException("maxIterations cannot be negative");
			}
			if (options.getCostLimit() != null && options.getCostLimit() < 0) {
				throw new IllegalArgumentException("costLimit cannot be negative");
			}
			return options;
		}

	}

	// Convenience factory methods for common configurations

	/**
	 * Creates SweAgentOptions with sensible defaults for autonomous operation.
	 * @return SweAgentOptions with yolo mode enabled and 5-minute timeout
	 */
	public static SweAgentOptions defaultOptions() {
		return new SweAgentOptions();
	}

	/**
	 * Creates SweAgentOptions optimized for fast responses with minimal latency. Uses
	 * shorter timeout and fewer iterations.
	 * @return SweAgentOptions configured for speed
	 */
	public static SweAgentOptions fastResponse() {
		return builder().model("gpt-4o-mini")
			.yoloMode(true)
			.exitImmediately(true)
			.timeout(Duration.ofMinutes(2))
			.maxIterations(10)
			.build();
	}

	/**
	 * Creates SweAgentOptions optimized for high-quality, complex responses. Uses more
	 * capable model with extended timeout and iterations.
	 * @return SweAgentOptions configured for quality
	 */
	public static SweAgentOptions highQuality() {
		return builder().model("gpt-4o")
			.yoloMode(true)
			.exitImmediately(true)
			.timeout(Duration.ofMinutes(10))
			.maxIterations(50)
			.build();
	}

	/**
	 * Creates SweAgentOptions optimized for development and testing scenarios. Uses fast
	 * model with debug logging enabled and lower cost limits.
	 * @return SweAgentOptions configured for development
	 */
	public static SweAgentOptions development() {
		return builder().model("gpt-4o-mini")
			.yoloMode(true)
			.exitImmediately(true)
			.verbose(true)
			.timeout(Duration.ofMinutes(3))
			.maxIterations(15)
			.costLimit(1.0) // $1 limit for development
			.build();
	}

	/**
	 * Creates SweAgentOptions with interactive mode disabled but confirmation enabled.
	 * Suitable for semi-autonomous operation where human oversight is desired.
	 * @return SweAgentOptions configured for supervised execution
	 */
	public static SweAgentOptions supervised() {
		return builder().model("gpt-4o")
			.yoloMode(false) // Require confirmation
			.exitImmediately(false)
			.verbose(true)
			.timeout(Duration.ofMinutes(15))
			.maxIterations(30)
			.build();
	}

}