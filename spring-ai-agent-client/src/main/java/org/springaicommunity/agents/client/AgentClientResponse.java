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

import java.util.Objects;

import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;

/**
 * Client-level response wrapper, equivalent to ChatResponse in Spring AI.
 *
 * <p>
 * This provides a simplified view of the agent response at the client API level, while
 * wrapping the underlying model-layer AgentResponse.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class AgentClientResponse {

	private final AgentResponse agentResponse;

	/**
	 * Create a client response wrapping the model response.
	 * @param agentResponse the underlying model response
	 */
	public AgentClientResponse(AgentResponse agentResponse) {
		this.agentResponse = Objects.requireNonNull(agentResponse, "AgentResponse cannot be null");
	}

	/**
	 * Primary outcome string.
	 * @return the primary result text
	 */
	public String getResult() {
		return this.agentResponse.getResult() != null ? this.agentResponse.getResult().getOutput() : "";
	}

	/**
	 * Access structured model-layer response.
	 * @return the underlying agent response
	 */
	public AgentResponse getAgentResponse() {
		return this.agentResponse;
	}

	/**
	 * Get the response metadata.
	 * @return the response metadata
	 */
	public AgentResponseMetadata getMetadata() {
		return this.agentResponse.getMetadata();
	}

	/**
	 * Check if the agent task was successful.
	 * @return true if successful
	 */
	public boolean isSuccessful() {
		return this.agentResponse.getResult() != null
				&& "SUCCESS".equals(this.agentResponse.getResult().getMetadata().getFinishReason());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AgentClientResponse that))
			return false;
		return Objects.equals(this.agentResponse, that.agentResponse);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.agentResponse);
	}

	@Override
	public String toString() {
		return "AgentClientResponse[" + "result='" + getResult() + '\'' + ", successful=" + isSuccessful() + ']';
	}

}