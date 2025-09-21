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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springaicommunity.agents.model.sandbox.ExecSpecCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for sandbox environments.
 *
 * <p>
 * Allows configuration of sandbox behavior through application properties in the
 * {@code spring.ai.agents.sandbox} namespace.
 *
 * <p>
 * Example configuration:
 *
 * <pre>
 * spring:
 *   ai:
 *     agents:
 *       sandbox:
 *         docker:
 *           enabled: true
 *           image-tag: "ghcr.io/spring-ai-community/agents-runtime:latest"
 *         local:
 *           working-directory: "/tmp/agent-workspace"
 * </pre>
 *
 * @author Spring AI Community
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.ai.agents.sandbox")
public class SandboxProperties {

	/**
	 * Docker sandbox configuration.
	 */
	private final Docker docker = new Docker();

	/**
	 * Local sandbox configuration.
	 */
	private final Local local = new Local();

	public Docker getDocker() {
		return docker;
	}

	public Local getLocal() {
		return local;
	}

	/**
	 * Docker sandbox specific configuration.
	 */
	public static class Docker {

		/**
		 * Whether Docker sandbox is enabled. Defaults to true if Docker is available.
		 */
		private boolean enabled = true;

		/**
		 * Docker image tag to use for the sandbox container.
		 */
		private String imageTag = "ghcr.io/spring-ai-community/agents-runtime:latest";

		/**
		 * Custom execution customizers for the Docker sandbox.
		 */
		private List<ExecSpecCustomizer> customizers = new ArrayList<>();

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getImageTag() {
			return imageTag;
		}

		public void setImageTag(String imageTag) {
			this.imageTag = imageTag;
		}

		public List<ExecSpecCustomizer> getCustomizers() {
			return customizers;
		}

		public void setCustomizers(List<ExecSpecCustomizer> customizers) {
			this.customizers = customizers;
		}

	}

	/**
	 * Local sandbox specific configuration.
	 */
	public static class Local {

		/**
		 * Working directory for local sandbox execution.
		 */
		private Path workingDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "agent-workspace");

		public Path getWorkingDirectory() {
			return workingDirectory;
		}

		public void setWorkingDirectory(Path workingDirectory) {
			this.workingDirectory = workingDirectory;
		}

	}

}