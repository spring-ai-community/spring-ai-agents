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
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentOptions;
import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;

/**
 * Command line runner that demonstrates a simple hello world example using Spring AI Agents.
 * 
 * This runner:
 * 1. Sets up Claude Code client and agent model
 * 2. Uses AgentClient to create a hello.txt file in the current directory
 * 3. Verifies the file was created and displays the contents
 * 
 * @author Spring AI Community
 */
@Component
public class HelloWorldRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldRunner.class);
    private static final String HELLO_WORLD_GOAL = "Create a simple hello.txt file with the content 'Hello, World!'";

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Spring AI Agents Hello World sample...");

        // Check if API key is set
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.error("ANTHROPIC_API_KEY environment variable is not set!");
            log.error("Please set your API key: export ANTHROPIC_API_KEY='your-api-key-here'");
            return;
        }

        try {
            // 1. Create Claude Code client (uses current directory by default)
            ClaudeCodeClient claudeClient = ClaudeCodeClient.create();

            // 2. Configure agent options
            ClaudeCodeAgentOptions options = ClaudeCodeAgentOptions.builder()
                .model("claude-sonnet-4-0")
                .yolo(true) // Allow agent to make changes
                .build();

            // 3. Create agent model
            ClaudeCodeAgentModel agentModel = new ClaudeCodeAgentModel(claudeClient, options);

            // 4. Check if agent is available
            if (!agentModel.isAvailable()) {
                log.error("Claude Code agent is not available. Please ensure Claude CLI is installed:");
                log.error("npm install -g @anthropic-ai/claude-code");
                return;
            }

            // 5. Create AgentClient and execute goal
            AgentClient agentClient = AgentClient.create(agentModel);
            
            log.info("Executing goal: {}", HELLO_WORLD_GOAL);
            AgentClientResponse response = agentClient.goal(HELLO_WORLD_GOAL).run();

            // 6. Check results
            if (response.isSuccessful()) {
                log.info("✅ Goal completed successfully!");
                log.info("Agent response: {}", response.getResult());
            } else {
                log.error("❌ Goal execution failed: {}", response.getResult());
            }

        } catch (Exception e) {
            log.error("❌ Error running hello world sample: {}", e.getMessage(), e);
            throw e;
        }
    }


}