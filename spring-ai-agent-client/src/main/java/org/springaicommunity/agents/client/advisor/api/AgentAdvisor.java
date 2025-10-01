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

import org.springframework.core.Ordered;

/**
 * Parent advisor interface for all agent advisors. Follows the Spring AI advisor pattern
 * for consistent integration with the Spring AI ecosystem.
 *
 * <p>
 * Advisors allow intercepting and augmenting agent execution flow, enabling use cases
 * like:
 * <ul>
 * <li>Context injection (vendir, git repos, etc.)
 * <li>Post-execution evaluation (judges, validators)
 * <li>Metrics collection and logging
 * <li>Request/response transformation
 * </ul>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see AgentCallAdvisor
 */
public interface AgentAdvisor extends Ordered {

	/**
	 * Default precedence order for agent advisors. Ensures this order has lower priority
	 * than Spring AI internal advisors, leaving room (1000 slots) for custom advisors
	 * with higher priority.
	 */
	int DEFAULT_AGENT_PRECEDENCE_ORDER = Ordered.HIGHEST_PRECEDENCE + 1000;

	/**
	 * Return the name of the advisor for identification and logging.
	 * @return the advisor name
	 */
	String getName();

}
