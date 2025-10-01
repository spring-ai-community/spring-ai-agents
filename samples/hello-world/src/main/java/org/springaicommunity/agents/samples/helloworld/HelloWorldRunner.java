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

package org.springaicommunity.agents.samples.helloworld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;

/**
 * Command line runner that demonstrates a simple hello world example using Spring AI
 * Agents with Spring Boot auto-configuration.
 *
 * <p>
 * This runner uses Spring Boot auto-configuration to automatically set up: - Claude agent
 * model (spring.ai.claude-agent.*) - Local or Docker sandbox (spring.ai.sandbox.*) -
 * AgentClient bean
 * </p>
 *
 * <p>
 * Prerequisites: - ANTHROPIC_API_KEY environment variable or Claude CLI session
 * authentication - Claude CLI installed: npm install -g @anthropic-ai/claude-code
 * </p>
 *
 * @author Spring AI Community
 */
@Component
public class HelloWorldRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(HelloWorldRunner.class);

	private final AgentClient agentClient;

	public HelloWorldRunner(AgentClient agentClient) {
		this.agentClient = agentClient;
	}

	@Override
	public void run(String... args) {
		log.info("Starting Spring AI Agents Hello World sample...");

		String goal = "Create a simple hello.txt file with the content 'Hello, World!'";

		log.info("Executing goal: {}", goal);
		AgentClientResponse response = agentClient.run(goal);

		if (response.isSuccessful()) {
			log.info("✅ Goal completed successfully!");
			log.info("Agent response: {}", response.getResult());
		}
		else {
			log.error("❌ Goal execution failed: {}", response.getResult());
		}
	}

}