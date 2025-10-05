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

package org.springaicommunity.agents.amp.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Amp Agent Model.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
@ConfigurationProperties(prefix = "spring.ai.agents.amp")
public class AmpAgentProperties {

	/**
	 * Model to use (if Amp CLI supports model selection).
	 */
	private String model = "amp-default";

	/**
	 * Timeout for agent task execution.
	 */
	private Duration timeout = Duration.ofMinutes(5);

	/**
	 * Whether to enable "dangerously allow all" mode (bypass all permission checks).
	 */
	private boolean dangerouslyAllowAll = true;

	/**
	 * Path to the Amp CLI executable. If null, auto-discovery is used.
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

	public boolean isDangerouslyAllowAll() {
		return dangerouslyAllowAll;
	}

	public void setDangerouslyAllowAll(boolean dangerouslyAllowAll) {
		this.dangerouslyAllowAll = dangerouslyAllowAll;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

}
