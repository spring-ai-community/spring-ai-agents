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
package org.springaicommunity.agents.client.autoconfigure;

import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.model.AgentModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * Spring Boot auto-configuration for {@link AgentClient}.
 *
 * <p>
 * This provides both a default {@link AgentClient} bean and a prototype-scoped
 * {@link AgentClient.Builder} bean, following Spring AI's ChatClient pattern.
 *
 * <p>
 * Requires an {@link AgentModel} bean to be present in the application context, which is
 * typically provided by a model-specific autoconfiguration (e.g.,
 * ClaudeAgentAutoConfiguration).
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(AgentClient.class)
@ConditionalOnBean(AgentModel.class)
public class AgentClientAutoConfiguration {

	/**
	 * Creates a default AgentClient bean.
	 * @param agentModel the configured agent model
	 * @return a configured AgentClient instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public AgentClient agentClient(AgentModel agentModel) {
		return AgentClient.builder(agentModel).build();
	}

	/**
	 * Creates an AgentClient.Builder with prototype scope.
	 * @param agentModel the configured agent model
	 * @return a new builder instance for each injection point
	 */
	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean(name = "agentClientBuilder")
	public AgentClient.Builder agentClientBuilder(AgentModel agentModel) {
		return AgentClient.builder(agentModel);
	}

}
