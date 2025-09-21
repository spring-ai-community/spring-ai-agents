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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.model.sandbox.DockerSandbox;
import org.springaicommunity.agents.model.sandbox.LocalSandbox;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.DockerClientFactory;

/**
 * Spring Boot auto-configuration for sandbox environments used by AI agents.
 *
 * <p>
 * Provides automatic configuration of sandbox implementations with the following
 * priority:
 * <ol>
 * <li>User-defined {@link Sandbox} bean (highest priority via
 * {@link ConditionalOnMissingBean})</li>
 * <li>Docker sandbox when Docker is available and enabled</li>
 * <li>Local sandbox as fallback</li>
 * </ol>
 *
 * <p>
 * Configuration is controlled via properties in the {@code spring.ai.agents.sandbox}
 * namespace.
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SandboxProperties.class)
public class SandboxAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(SandboxAutoConfiguration.class);

	/**
	 * Creates a Docker-based sandbox when Docker is available and not explicitly
	 * disabled. This is the preferred sandbox implementation for secure isolation.
	 * @param properties sandbox configuration properties
	 * @return configured Docker sandbox
	 */
	@Bean
	@Primary
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "org.testcontainers.DockerClientFactory")
	@ConditionalOnProperty(name = "spring.ai.agents.sandbox.docker.enabled", havingValue = "true",
			matchIfMissing = true)
	public Sandbox dockerSandbox(SandboxProperties properties) {
		try {
			// Verify Docker is actually available
			DockerClientFactory.instance().client();

			String imageTag = properties.getDocker().getImageTag();
			logger.info("Creating DockerSandbox with image: {}", imageTag);

			return new DockerSandbox(imageTag, properties.getDocker().getCustomizers());
		}
		catch (Exception e) {
			logger.warn("Docker is not available, falling back to LocalSandbox: {}", e.getMessage());
			return createLocalSandbox(properties);
		}
	}

	/**
	 * Creates a local sandbox as fallback when Docker is not available or disabled.
	 * @param properties sandbox configuration properties
	 * @return configured local sandbox
	 */
	@Bean
	@ConditionalOnMissingBean
	public Sandbox localSandbox(SandboxProperties properties) {
		return createLocalSandbox(properties);
	}

	private LocalSandbox createLocalSandbox(SandboxProperties properties) {
		logger.info("Creating LocalSandbox with working directory: {}", properties.getLocal().getWorkingDirectory());
		return new LocalSandbox(properties.getLocal().getWorkingDirectory());
	}

}