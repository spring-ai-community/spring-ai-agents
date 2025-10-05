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

package org.springaicommunity.agents.codex.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Codex Agent Model.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
@ConfigurationProperties(prefix = "spring.ai.agents.codex")
public class CodexAgentProperties {

	/**
	 * Model to use for Codex execution.
	 */
	private String model = "gpt-5-codex";

	/**
	 * Timeout for agent task execution.
	 */
	private Duration timeout = Duration.ofMinutes(5);

	/**
	 * Enable full-auto mode (workspace-write sandbox + never approval).
	 */
	private boolean fullAuto = true;

	/**
	 * Skip git repository check (use with caution).
	 */
	private boolean skipGitCheck = false;

	/**
	 * Path to the Codex CLI executable. If null, auto-discovery is used.
	 */
	private String executablePath;

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

	public boolean isFullAuto() {
		return fullAuto;
	}

	public void setFullAuto(boolean fullAuto) {
		this.fullAuto = fullAuto;
	}

	public boolean isSkipGitCheck() {
		return skipGitCheck;
	}

	public void setSkipGitCheck(boolean skipGitCheck) {
		this.skipGitCheck = skipGitCheck;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

}
