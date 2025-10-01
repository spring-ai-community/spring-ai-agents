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

import java.util.HashMap;
import java.util.Map;

import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;

/**
 * Client-layer response type for agent execution flows with advisor support. Provides a
 * context map for advisors to share data and evaluation results.
 *
 * <p>
 * Follows the Spring AI ChatClientResponse pattern for consistency with the Spring AI
 * ecosystem.
 *
 * @param agentResponse the underlying agent model response
 * @param context mutable context map for advisors (evaluation results, metrics, etc.)
 * @author Mark Pollack
 * @since 0.1.0
 */
public record AgentClientResponse(AgentResponse agentResponse, Map<String, Object> context) {

	/**
	 * Convenience constructor with empty context map.
	 * @param agentResponse the underlying agent model response
	 */
	public AgentClientResponse(AgentResponse agentResponse) {
		this(agentResponse, new HashMap<>());
	}

	/**
	 * Primary outcome string (backward compatibility method).
	 * @return the primary result text
	 */
	public String getResult() {
		return this.agentResponse.getResult() != null ? this.agentResponse.getResult().getOutput() : "";
	}

	/**
	 * Access structured model-layer response (backward compatibility method).
	 * @return the underlying agent response
	 */
	public AgentResponse getAgentResponse() {
		return this.agentResponse;
	}

	/**
	 * Get the response metadata (backward compatibility method).
	 * @return the response metadata
	 */
	public AgentResponseMetadata getMetadata() {
		return this.agentResponse.getMetadata();
	}

	/**
	 * Check if the agent task was successful (backward compatibility method).
	 * @return true if successful
	 */
	public boolean isSuccessful() {
		return this.agentResponse.getResult() != null
				&& "SUCCESS".equals(this.agentResponse.getResult().getMetadata().getFinishReason());
	}

}