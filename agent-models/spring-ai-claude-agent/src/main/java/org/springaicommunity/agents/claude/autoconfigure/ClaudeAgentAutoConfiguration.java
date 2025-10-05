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
package org.springaicommunity.agents.claude.autoconfigure;

import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.claude.sdk.ClaudeAgentClient;
import org.springaicommunity.agents.model.AgentModel;
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
@ConditionalOnClass(ClaudeAgentModel.class)
@EnableConfigurationProperties(ClaudeAgentProperties.class)
public class ClaudeAgentAutoConfiguration {

	/**
	 * Creates a Claude Code client for interfacing with the Claude CLI.
	 * @param properties agent configuration properties
	 * @return configured Claude Code client
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClaudeAgentClient claudeCodeClient(ClaudeAgentProperties properties) {
		try {
			return ClaudeAgentClient.create(properties.buildCLIOptions());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create ClaudeAgentClient", e);
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
	public AgentModel agentModel(ClaudeAgentClient claudeCodeClient, ClaudeAgentProperties properties,
			ObjectProvider<Sandbox> sandboxProvider) {

		ClaudeAgentOptions options = ClaudeAgentOptions.builder()
			.model(properties.getModel())
			.timeout(properties.getTimeout())
			.yolo(properties.isYolo())
			.executablePath(properties.getExecutablePath())
			.build();

		return new ClaudeAgentModel(claudeCodeClient, options, sandboxProvider.getIfAvailable());
	}

}