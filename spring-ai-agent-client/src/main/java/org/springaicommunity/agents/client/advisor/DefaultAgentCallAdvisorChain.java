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

package org.springaicommunity.agents.client.advisor;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springframework.core.OrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Default implementation of {@link AgentCallAdvisorChain} using a Deque-based
 * chain-of-responsibility pattern.
 *
 * <p>
 * Follows Spring AI's DefaultAroundAdvisorChain design for consistency.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class DefaultAgentCallAdvisorChain implements AgentCallAdvisorChain {

	private final List<AgentCallAdvisor> originalCallAdvisors;

	private final Deque<AgentCallAdvisor> callAdvisors;

	DefaultAgentCallAdvisorChain(Deque<AgentCallAdvisor> callAdvisors) {
		Assert.notNull(callAdvisors, "the callAdvisors must be non-null");
		this.callAdvisors = callAdvisors;
		this.originalCallAdvisors = List.copyOf(callAdvisors);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public AgentClientResponse nextCall(AgentClientRequest request) {
		Assert.notNull(request, "the request cannot be null");

		if (this.callAdvisors.isEmpty()) {
			throw new IllegalStateException("No AgentCallAdvisors available to execute");
		}

		var advisor = this.callAdvisors.pop();
		return advisor.adviseCall(request, this);
	}

	@Override
	public List<AgentCallAdvisor> getCallAdvisors() {
		return this.originalCallAdvisors;
	}

	public static class Builder {

		private final Deque<AgentCallAdvisor> callAdvisors;

		public Builder() {
			this.callAdvisors = new ConcurrentLinkedDeque<>();
		}

		public Builder push(AgentCallAdvisor advisor) {
			Assert.notNull(advisor, "the advisor must be non-null");
			return this.pushAll(List.of(advisor));
		}

		public Builder pushAll(List<AgentCallAdvisor> advisors) {
			Assert.notNull(advisors, "the advisors must be non-null");
			Assert.noNullElements(advisors, "the advisors must not contain null elements");
			if (!CollectionUtils.isEmpty(advisors)) {
				advisors.forEach(this.callAdvisors::push);
				this.reOrder();
			}
			return this;
		}

		/**
		 * (Re)orders the advisors in priority order based on their Ordered attribute.
		 */
		private void reOrder() {
			ArrayList<AgentCallAdvisor> callAdvisors = new ArrayList<>(this.callAdvisors);
			OrderComparator.sort(callAdvisors);
			this.callAdvisors.clear();
			callAdvisors.forEach(this.callAdvisors::addLast);
		}

		public DefaultAgentCallAdvisorChain build() {
			return new DefaultAgentCallAdvisorChain(this.callAdvisors);
		}

	}

}
