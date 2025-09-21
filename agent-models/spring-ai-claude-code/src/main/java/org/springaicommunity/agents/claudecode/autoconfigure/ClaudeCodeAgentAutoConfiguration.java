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
package org.springaicommunity.agents.claudecode.autoconfigure;

import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentOptions;
import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for Claude Code agent model.
 *
 * <p>
 * Provides automatic configuration of Claude Code agents following Spring AI patterns.
 * The agent model uses a sandbox for secure command execution and integrates with Claude
 * Code CLI for AI-powered development tasks.
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ClaudeCodeAgentModel.class)
@EnableConfigurationProperties(ClaudeCodeAgentProperties.class)
public class ClaudeCodeAgentAutoConfiguration {

	/**
	 * Creates a Claude Code client for interfacing with the Claude CLI.
	 * @param properties agent configuration properties
	 * @return configured Claude Code client
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClaudeCodeClient claudeCodeClient(ClaudeCodeAgentProperties properties) {
		try {
			return ClaudeCodeClient.create(properties.buildCLIOptions());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create ClaudeCodeClient", e);
		}
	}

	/**
	 * Creates a Claude Code agent model with automatic dependency injection.
	 * @param claudeCodeClient the Claude Code CLI client
	 * @param properties agent configuration properties
	 * @param sandboxProvider sandbox for secure command execution
	 * @return configured Claude Code agent model
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClaudeCodeAgentModel claudeCodeAgentModel(ClaudeCodeClient claudeCodeClient,
			ClaudeCodeAgentProperties properties, ObjectProvider<Sandbox> sandboxProvider) {

		ClaudeCodeAgentOptions options = ClaudeCodeAgentOptions.builder()
			.model(properties.getModel())
			.timeout(properties.getTimeout())
			.yolo(properties.isYolo())
			.executablePath(properties.getExecutablePath())
			.build();

		return new ClaudeCodeAgentModel(claudeCodeClient, options, sandboxProvider.getIfAvailable());
	}

}