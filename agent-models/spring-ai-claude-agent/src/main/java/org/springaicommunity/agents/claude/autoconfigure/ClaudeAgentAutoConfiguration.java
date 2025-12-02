/*
 * Copyright 2025 Spring AI Community
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
 * The agent model integrates with Claude Code CLI for AI-powered development tasks,
 * supporting blocking, streaming, and iterator-based programming models.
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ClaudeAgentModel.class)
@EnableConfigurationProperties(ClaudeAgentProperties.class)
public class ClaudeAgentAutoConfiguration {

	/**
	 * Creates a Claude Code agent model with automatic dependency injection.
	 * <p>
	 * The model implements all three programming styles:
	 * <ul>
	 * <li>{@link org.springaicommunity.agents.model.AgentModel} - Blocking</li>
	 * <li>{@link org.springaicommunity.agents.model.StreamingAgentModel} - Reactive</li>
	 * <li>{@link org.springaicommunity.agents.model.IterableAgentModel} - Iterator</li>
	 * </ul>
	 * @param properties agent configuration properties
	 * @return configured Claude Code agent model
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClaudeAgentModel claudeAgentModel(ClaudeAgentProperties properties) {
		ClaudeAgentOptions options = ClaudeAgentOptions.builder()
			.model(properties.getModel())
			.timeout(properties.getTimeout())
			.yolo(properties.isYolo())
			.executablePath(properties.getExecutablePath())
			.build();

		ClaudeAgentModel.Builder builder = ClaudeAgentModel.builder()
			.timeout(properties.getTimeout())
			.defaultOptions(options);

		if (properties.getExecutablePath() != null) {
			builder.claudePath(properties.getExecutablePath());
		}

		return builder.build();
	}

}