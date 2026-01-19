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

import org.springaicommunity.agents.amazonq.AmazonQAgentModel;
import org.springaicommunity.agents.amazonq.AmazonQAgentOptions;
import org.springaicommunity.agents.amazonqsdk.AmazonQClient;
import org.springaicommunity.sandbox.Sandbox;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * Auto-configuration for Amazon Q Agent Model.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
@Configuration
@ConditionalOnClass(AmazonQClient.class)
@EnableConfigurationProperties(AmazonQAgentProperties.class)
public class AmazonQAgentAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AmazonQClient amazonQClient(AmazonQAgentProperties properties) {
		if (properties.getExecutablePath() != null) {
			return AmazonQClient.create(Paths.get(System.getProperty("user.dir")), properties.getExecutablePath());
		}
		return AmazonQClient.create();
	}

	@Bean
	@ConditionalOnMissingBean
	public AmazonQAgentOptions amazonQAgentOptions(AmazonQAgentProperties properties) {
		return AmazonQAgentOptions.builder()
			.model(properties.getModel())
			.timeout(properties.getTimeout())
			.trustAllTools(properties.isTrustAllTools())
			.trustTools(properties.getTrustTools())
			.agent(properties.getAgent())
			.verbose(properties.isVerbose())
			.executablePath(properties.getExecutablePath())
			.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public AmazonQAgentModel amazonQAgentModel(AmazonQClient amazonQClient, AmazonQAgentOptions amazonQAgentOptions,
			Sandbox sandbox) {
		return new AmazonQAgentModel(amazonQClient, amazonQAgentOptions, sandbox);
	}

}
