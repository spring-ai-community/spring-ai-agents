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

package org.springaicommunity.agents.amazonq.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Amazon Q Agent.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
@ConfigurationProperties(prefix = "spring.ai.agents.amazon-q")
public class AmazonQAgentProperties {

	/**
	 * The model to use for Amazon Q.
	 */
	private String model = "amazon-q-developer";

	/**
	 * Execution timeout.
	 */
	private Duration timeout = Duration.ofMinutes(10);

	/**
	 * Whether to trust all tools for autonomous execution.
	 */
	private boolean trustAllTools = true;

	/**
	 * Specific tools to trust (overrides trustAllTools if set).
	 */
	private List<String> trustTools = new ArrayList<>();

	/**
	 * Agent/context profile to use.
	 */
	private String agent;

	/**
	 * Enable verbose logging.
	 */
	private boolean verbose = false;

	/**
	 * Path to Amazon Q CLI executable (optional, auto-discovered if not set).
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

	public boolean isTrustAllTools() {
		return trustAllTools;
	}

	public void setTrustAllTools(boolean trustAllTools) {
		this.trustAllTools = trustAllTools;
	}

	public List<String> getTrustTools() {
		return trustTools;
	}

	public void setTrustTools(List<String> trustTools) {
		this.trustTools = trustTools;
	}

	public String getAgent() {
		return agent;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

}
