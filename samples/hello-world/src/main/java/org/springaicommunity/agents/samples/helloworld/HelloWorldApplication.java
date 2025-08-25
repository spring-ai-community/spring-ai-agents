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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hello World sample application demonstrating Spring AI Agents with Claude Code.
 *
 * This sample shows how to: - Set up a Spring Boot application with Spring AI Agents -
 * Configure Claude Code agent model - Use AgentClient to execute a simple goal
 *
 * Prerequisites: - ANTHROPIC_API_KEY environment variable must be set - Claude CLI must
 * be installed: npm install -g @anthropic-ai/claude-code
 *
 * @author Spring AI Community
 */
@SpringBootApplication
public class HelloWorldApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelloWorldApplication.class, args);
	}

}