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

import org.springaicommunity.agents.codex.CodexAgentModel;
import org.springaicommunity.agents.codex.CodexAgentOptions;
import org.springaicommunity.agents.codexsdk.CodexClient;
import org.springaicommunity.agents.codexsdk.types.ExecuteOptions;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for Codex agent model.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(CodexAgentModel.class)
@EnableConfigurationProperties(CodexAgentProperties.class)
public class CodexAgentAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public CodexClient codexClient(CodexAgentProperties properties) {
		ExecuteOptions options = ExecuteOptions.builder()
			.model(properties.getModel())
			.timeout(properties.getTimeout())
			.fullAuto(properties.isFullAuto())
			.skipGitCheck(properties.isSkipGitCheck())
			.build();

		return CodexClient.create(options);
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentModel agentModel(CodexClient codexClient, CodexAgentProperties properties,
			ObjectProvider<Sandbox> sandboxProvider) {

		CodexAgentOptions options = CodexAgentOptions.builder()
			.model(properties.getModel())
			.timeout(properties.getTimeout())
			.fullAuto(properties.isFullAuto())
			.skipGitCheck(properties.isSkipGitCheck())
			.executablePath(properties.getExecutablePath())
			.build();

		return new CodexAgentModel(codexClient, options, sandboxProvider.getIfAvailable());
	}

}
