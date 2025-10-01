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

package org.springaicommunity.agents.claude;

import java.util.List;

/**
 * Definition for a programmatic subagent. Allows defining agents inline without
 * filesystem dependencies.
 *
 * @param description agent description
 * @param prompt the system prompt for this agent
 * @param tools allowed tools for this agent (null means inherit from parent)
 * @param model model to use: "sonnet", "opus", "haiku", or "inherit" (null means inherit)
 * @author Mark Pollack
 * @since 1.1.0
 */
public record AgentDefinition(String description, String prompt, List<String> tools, String model) {

	/**
	 * Creates an agent definition with minimal parameters.
	 * @param description agent description
	 * @param prompt the system prompt
	 */
	public AgentDefinition(String description, String prompt) {
		this(description, prompt, null, null);
	}

	/**
	 * Creates an agent definition with tools.
	 * @param description agent description
	 * @param prompt the system prompt
	 * @param tools allowed tools
	 */
	public AgentDefinition(String description, String prompt, List<String> tools) {
		this(description, prompt, tools, null);
	}

}
