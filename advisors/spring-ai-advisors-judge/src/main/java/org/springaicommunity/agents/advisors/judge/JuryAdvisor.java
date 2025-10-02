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
import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.jury.Jury;
import org.springaicommunity.agents.judge.jury.Verdict;

/**
 * Advisor that evaluates agent execution results using a {@link Jury}.
 *
 * <p>
 * Integrates the Jury API as a post-processing advisor, executing after the agent call
 * completes to aggregate judgments from multiple judges. The verdict is attached to the
 * response context for downstream processing.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // With Spring DI (preferred)
 * &#64;Bean
 * public JuryAdvisor ensembleAdvisor(Jury jury) {
 *     return JuryAdvisor.builder()
 *         .jury(jury)
 *         .build();
 * }
 *
 * // Programmatic usage
 * Jury jury = SimpleJury.builder()
 *     .judge(fileJudge, 0.3)
 *     .judge(buildJudge, 0.7)
 *     .votingStrategy(new WeightedAverageStrategy())
 *     .build();
 *
 * JuryAdvisor advisor = JuryAdvisor.builder()
 *     .jury(jury)
 *     .order(DEFAULT_AGENT_PRECEDENCE_ORDER + 100)
 *     .build();
 *
 * agentClient
 *     .advisors(advisor)
 *     .call();
 * }</pre>
 *
 * <p>
 * The verdict is stored in the response context under the following keys:
 * </p>
 * <ul>
 * <li>{@code verdict} - The complete {@link Verdict} object</li>
 * <li>{@code verdict.aggregated} - The aggregated
 * {@link org.springaicommunity.agents.judge.result.Judgment}</li>
 * <li>{@code verdict.pass} - Boolean indicating pass/fail based on aggregated
 * judgment</li>
 * <li>{@code verdict.status} - The
 * {@link org.springaicommunity.agents.judge.result.JudgmentStatus}</li>
 * </ul>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see Jury
 * @see Verdict
 */
public class JuryAdvisor implements AgentCallAdvisor {

	private final Jury jury;

	private final int order;

	/**
	 * Create a JuryAdvisor with the specified jury and order.
	 * @param jury the jury to use for evaluation
	 * @param order the advisor order (higher = later execution)
	 */
	public JuryAdvisor(Jury jury, int order) {
		if (jury == null) {
			throw new IllegalArgumentException("Jury cannot be null");
		}
		this.jury = jury;
		this.order = order;
	}

	/**
	 * Create a JuryAdvisor with the specified jury and default order.
	 * @param jury the jury to use for evaluation
	 */
	public JuryAdvisor(Jury jury) {
		this(jury, DEFAULT_AGENT_PRECEDENCE_ORDER + 100);
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

		// Execute jury voting
		Verdict verdict = jury.vote(context);

		// Attach verdict to response context
		response.context().put("verdict", verdict);
		response.context().put("verdict.aggregated", verdict.aggregated());
		response.context().put("verdict.pass", verdict.aggregated().pass());
		response.context().put("verdict.status", verdict.aggregated().status());

		return response;
	}

	@Override
	public String getName() {
		return "JuryAdvisor[" + jury.getVotingStrategy().getName() + "]";
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
	 * Builder for creating JuryAdvisor instances.
	 */
	public static class Builder {

		private Jury jury;

		private int order = DEFAULT_AGENT_PRECEDENCE_ORDER + 100;

		/**
		 * Set the jury to use for evaluation.
		 * @param jury the jury
		 * @return this builder
		 */
		public Builder jury(Jury jury) {
			this.jury = jury;
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
		 * Build the JuryAdvisor.
		 * @return the configured JuryAdvisor
		 */
		public JuryAdvisor build() {
			if (jury == null) {
				throw new IllegalArgumentException("Jury must be specified");
			}
			return new JuryAdvisor(jury, order);
		}

	}

}
