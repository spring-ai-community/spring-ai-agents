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

package org.springaicommunity.agents.advisors.judge;

import java.time.Duration;
import java.time.Instant;

import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springaicommunity.agents.judge.Judge;
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;

/**
 * Advisor that evaluates agent execution results using a {@link Judge}.
 *
 * <p>
 * Integrates the Judge API as a post-processing advisor, executing after the agent call
 * completes to evaluate the results. The judgment is attached to the response context for
 * downstream processing.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // With Spring DI (preferred)
 * &#64;Bean
 * public JudgeAdvisor fileVerificationAdvisor(FileExistsJudge judge) {
 *     return JudgeAdvisor.builder()
 *         .judge(judge)
 *         .build();
 * }
 *
 * // Programmatic usage
 * JudgeAdvisor advisor = JudgeAdvisor.builder()
 *     .judge(new FileExistsJudge("output.txt"))
 *     .order(DEFAULT_AGENT_PRECEDENCE_ORDER + 100)
 *     .build();
 *
 * agentClient
 *     .advisors(advisor)
 *     .call();
 * }</pre>
 *
 * <p>
 * The judgment is stored in the response context under the following keys:
 * </p>
 * <ul>
 * <li>{@code judgment} - The complete {@link Judgment} object</li>
 * <li>{@code judgment.pass} - Boolean indicating pass/fail</li>
 * <li>{@code judgment.score} - The score object</li>
 * </ul>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see Judge
 * @see Judgment
 */
public class JudgeAdvisor implements AgentCallAdvisor {

	private final Judge judge;

	private final int order;

	/**
	 * Create a JudgeAdvisor with the specified judge and order.
	 * @param judge the judge to use for evaluation
	 * @param order the advisor order (higher = later execution)
	 */
	public JudgeAdvisor(Judge judge, int order) {
		if (judge == null) {
			throw new IllegalArgumentException("Judge cannot be null");
		}
		this.judge = judge;
		this.order = order;
	}

	/**
	 * Create a JudgeAdvisor with the specified judge and default order.
	 * @param judge the judge to use for evaluation
	 */
	public JudgeAdvisor(Judge judge) {
		this(judge, DEFAULT_AGENT_PRECEDENCE_ORDER + 100);
	}

	@Override
	public AgentClientResponse adviseCall(AgentClientRequest request, AgentCallAdvisorChain chain) {
		Instant startTime = Instant.now();

		// Execute the agent call
		AgentClientResponse response = chain.nextCall(request);

		Instant endTime = Instant.now();
		Duration executionTime = Duration.between(startTime, endTime);

		// Build judgment context from request and response
		JudgmentContext context = JudgmentContext.builder()
			.goal(request.goal().getContent())
			.workspace(request.workingDirectory())
			.agentOutput(response.getResult())
			.executionTime(executionTime)
			.startedAt(startTime)
			.status(determineStatus(response))
			.build();

		// Execute judge
		Judgment judgment = judge.judge(context);

		// Attach judgment to response context
		response.context().put("judgment", judgment);
		response.context().put("judgment.pass", judgment.pass());
		response.context().put("judgment.score", judgment.score());

		return response;
	}

	@Override
	public String getName() {
		return "JudgeAdvisor[" + judge.getMetadata().name() + "]";
	}

	@Override
	public int getOrder() {
		return order;
	}

	/**
	 * Determine agent execution status from response.
	 * @param response the agent client response
	 * @return the execution status
	 */
	private AgentExecutionStatus determineStatus(AgentClientResponse response) {
		if (response.isSuccessful()) {
			return AgentExecutionStatus.SUCCESS;
		}

		// Check for error/exception in metadata or context
		if (response.context().containsKey("error") || response.context().containsKey("exception")) {
			return AgentExecutionStatus.FAILED;
		}

		// Default to FAILED if not successful
		return AgentExecutionStatus.FAILED;
	}

	/**
	 * Create a builder for fluent construction.
	 * @return a new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating JudgeAdvisor instances.
	 */
	public static class Builder {

		private Judge judge;

		private int order = DEFAULT_AGENT_PRECEDENCE_ORDER + 100;

		/**
		 * Set the judge to use for evaluation.
		 * @param judge the judge
		 * @return this builder
		 */
		public Builder judge(Judge judge) {
			this.judge = judge;
			return this;
		}

		/**
		 * Set the advisor execution order.
		 * @param order the order (higher = later execution)
		 * @return this builder
		 */
		public Builder order(int order) {
			this.order = order;
			return this;
		}

		/**
		 * Build the JudgeAdvisor.
		 * @return the configured JudgeAdvisor
		 */
		public JudgeAdvisor build() {
			if (judge == null) {
				throw new IllegalArgumentException("Judge must be specified");
			}
			return new JudgeAdvisor(judge, order);
		}

	}

}
