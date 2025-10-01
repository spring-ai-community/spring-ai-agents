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

package org.springaicommunity.agents.client.advisor.api;

import java.util.List;

import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;

/**
 * A chain of {@link AgentCallAdvisor} instances orchestrating the execution of an
 * {@link AgentClientRequest} on the next {@link AgentCallAdvisor} in the chain.
 *
 * <p>
 * Follows the Spring AI advisor chain pattern for consistency with the Spring AI
 * ecosystem.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public interface AgentCallAdvisorChain {

	/**
	 * Invokes the next {@link AgentCallAdvisor} in the {@link AgentCallAdvisorChain} with
	 * the given request.
	 * @param request the agent client request
	 * @return the agent client response from the next advisor or terminal model call
	 */
	AgentClientResponse nextCall(AgentClientRequest request);

	/**
	 * Returns the list of all {@link AgentCallAdvisor} instances included in this chain
	 * at the time of its creation.
	 * @return the list of call advisors
	 */
	List<AgentCallAdvisor> getCallAdvisors();

}
