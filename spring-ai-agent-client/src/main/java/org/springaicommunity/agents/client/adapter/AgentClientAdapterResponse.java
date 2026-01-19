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

package org.springaicommunity.agents.client.adapter;

/**
 * Response from an AgentClientAdapter execution.
 *
 * <p>
 * This record provides a simple container for agent execution results that can be easily
 * adapted to other interfaces like spring-ai-judge's JudgeAgentResponse.
 * </p>
 *
 * @param result the result/output from the agent execution
 * @param successful whether the agent execution completed successfully
 * @author Mark Pollack
 * @since 0.1.0
 */
public record AgentClientAdapterResponse(String result, boolean successful) {

	/**
	 * Create a successful response with the given result.
	 * @param result the result of the successful execution
	 * @return a new AgentClientAdapterResponse marked as successful
	 */
	public static AgentClientAdapterResponse success(String result) {
		return new AgentClientAdapterResponse(result, true);
	}

	/**
	 * Create a failed response with the given result/error message.
	 * @param result the error message or partial result
	 * @return a new AgentClientAdapterResponse marked as failed
	 */
	public static AgentClientAdapterResponse failure(String result) {
		return new AgentClientAdapterResponse(result, false);
	}

}
