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

package org.springaicommunity.agents.client;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springaicommunity.agents.model.AgentOptions;

/**
 * Client-layer request type for agent execution flows with advisor support. Provides a
 * context map for advisors to share data across the execution chain.
 *
 * <p>
 * Follows the Spring AI ChatClientRequest pattern for consistency with the Spring AI
 * ecosystem.
 *
 * @param goal the goal to execute
 * @param workingDirectory the working directory for execution
 * @param options the agent configuration options
 * @param context mutable context map for advisors (vendir config, judge params, etc.)
 * @author Mark Pollack
 * @since 0.1.0
 */
public record AgentClientRequest(Goal goal, Path workingDirectory, AgentOptions options, Map<String, Object> context) {

	/**
	 * Convenience constructor with empty context map.
	 * @param goal the goal to execute
	 * @param workingDirectory the working directory for execution
	 * @param options the agent configuration options
	 */
	public AgentClientRequest(Goal goal, Path workingDirectory, AgentOptions options) {
		this(goal, workingDirectory, options, new HashMap<>());
	}

}
