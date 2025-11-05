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
package org.springaicommunity.agents.gemini.autoconfigure;

import java.time.Duration;

import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Gemini agent model.
 *
 * <p>
 * Allows configuration of Gemini agent behavior through application properties in the
 * {@code spring.ai.agents.gemini} namespace.
 *
 * <p>
 * Example configuration:
 *
 * <pre>
 * spring:
 *   ai:
 *     agents:
 *       gemini:
 *         model: "gemini-2.5-flash"
 *         timeout: "PT5M"
 *         yolo: true
 *         executable-path: "/usr/local/bin/gemini"
 * </pre>
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.ai.agents.gemini")
public class GeminiAgentProperties {

	/**
	 * Gemini model to use for agent tasks.
	 */
	private String model = "gemini-2.5-flash";

	/**
	 * Timeout for agent task execution.
	 */
	private Duration timeout = Duration.ofMinutes(5);

	/**
	 * Whether to enable "yolo" mode (bypass all permission checks).
	 */
	private boolean yolo = true;

	/**
	 * Path to the Gemini CLI executable.
	 */
	private String executablePath;

	/**
	 * Temperature for controlling response randomness (0.0-1.0).
	 */
	private Double temperature;

	/**
	 * Maximum number of tokens to generate in the response.
	 */
	private Integer maxTokens;

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public boolean isYolo() {
		return yolo;
	}

	public void setYolo(boolean yolo) {
		this.yolo = yolo;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	/**
	 * Builds CLI options from these properties.
	 * @return configured CLI options
	 */
	public CLIOptions buildCLIOptions() {
		CLIOptions.Builder builder = CLIOptions.builder().model(model).timeout(timeout);

		if (yolo) {
			builder.yoloMode(true);
		}

		if (temperature != null) {
			// Note: Temperature may not be supported by all Gemini CLI versions
		}

		if (maxTokens != null) {
			// Note: MaxTokens may not be supported by all Gemini CLI versions
		}

		return builder.build();
	}

}