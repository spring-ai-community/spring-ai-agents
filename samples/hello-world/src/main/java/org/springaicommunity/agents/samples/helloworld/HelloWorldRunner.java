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
 * AgentClient.Builder bean
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

	private final AgentClient.Builder agentClientBuilder;

	public HelloWorldRunner(AgentClient.Builder agentClientBuilder) {
		this.agentClientBuilder = agentClientBuilder;
	}

	@Override
	public void run(String... args) {
		log.info("Starting Spring AI Agents Hello World sample...");

		// Clean up from previous runs to ensure clean demonstration
		java.nio.file.Path targetFile = java.nio.file.Path.of("hello.txt");
		try {
			if (java.nio.file.Files.deleteIfExists(targetFile)) {
				log.info("Cleaned up hello.txt from previous run");
			}
		}
		catch (java.io.IOException e) {
			log.warn("Could not delete existing hello.txt: {}", e.getMessage());
		}

		String goal = "Create a simple hello.txt file with the content 'Hello, World!'";

		log.info("Executing goal: {}", goal);

		AgentClient agentClient = agentClientBuilder.build();
		AgentClientResponse response = agentClient.run(goal);

		if (response.isSuccessful()) {
			log.info("✅ Goal completed successfully!");
			log.info("Agent response: {}", response.getResult());

			// Verify the file was created
			if (java.nio.file.Files.exists(targetFile)) {
				log.info("✅ Verified: hello.txt was created successfully");
			}
		}
		else {
			log.error("❌ Goal execution failed: {}", response.getResult());
		}
	}

}