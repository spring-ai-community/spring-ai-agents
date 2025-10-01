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

import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;

/**
 * Advisor for execution flows ultimately resulting in a call to an agent model. Follows
 * the Spring AI advisor pattern for consistent integration.
 *
 * <p>
 * Call advisors operate in an "around" style, receiving the request, potentially
 * modifying it, calling the next advisor in the chain, and potentially modifying the
 * response before returning.
 *
 * <p>
 * Example use cases:
 * <ul>
 * <li>Pre-processing: Inject context (vendir sync, git clone)
 * <li>Post-processing: Evaluate results (judges, validators)
 * <li>Monitoring: Collect metrics and logs
 * <li>Transformation: Modify requests or responses
 * </ul>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public interface AgentCallAdvisor extends AgentAdvisor {

	/**
	 * Advise the agent call execution, potentially modifying the request before and/or
	 * the response after the next advisor in the chain is invoked.
	 * @param request the agent client request
	 * @param chain the advisor chain to continue execution
	 * @return the agent client response (potentially modified)
	 */
	AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain);

}
