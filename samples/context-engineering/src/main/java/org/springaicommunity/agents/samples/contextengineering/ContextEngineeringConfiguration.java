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

package org.springaicommunity.agents.samples.contextengineering;

import java.nio.file.Path;

import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.claude.sdk.ClaudeAgentClient;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.sandbox.LocalSandbox;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Context Engineering sample.
 *
 * <p>
 * Creates necessary beans for Claude agent execution including sandbox and agent model.
 * </p>
 *
 * @author Spring AI Community
 */
@Configuration
public class ContextEngineeringConfiguration {

	@Bean
	public Sandbox localSandbox() {
		return new LocalSandbox(Path.of(System.getProperty("user.dir")));
	}

	@Bean
	public ClaudeAgentClient claudeAgentClient() {
		try {
			return ClaudeAgentClient.create(CLIOptions.defaultOptions());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create Claude agent client", e);
		}
	}

	@Bean
	public AgentModel claudeAgentModel(Sandbox sandbox, ClaudeAgentClient claudeAgentClient) {
		ClaudeAgentOptions options = ClaudeAgentOptions.builder().model("claude-sonnet-4-20250514").yolo(true).build();

		return new ClaudeAgentModel(claudeAgentClient, options, sandbox);
	}

}
