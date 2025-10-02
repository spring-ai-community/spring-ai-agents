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

	/**
	 * Get the judgment result from JudgeAdvisor evaluation if present.
	 * <p>
	 * Provides first-class access to judgment results without requiring magic strings.
	 * Returns null if no JudgeAdvisor was used in the execution.
	 * </p>
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>{@code
	 * AgentClientResponse response = agentClient
	 *     .advisors(JudgeAdvisor.builder().judge(judge).build())
	 *     .run();
	 *
	 * Judgment judgment = response.getJudgment();
	 * if (judgment != null && judgment.pass()) {
	 *     // Task passed evaluation
	 * }
	 * }</pre>
	 * @param <T> the judgment type (typically Judgment from spring-ai-agents-judge)
	 * @return the judgment result, or null if no judgment was performed
	 */
	@SuppressWarnings("unchecked")
	public <T> T getJudgment() {
		return (T) this.context.get("judgment");
	}

	/**
	 * Check if a judgment was performed and passed.
	 * <p>
	 * Convenience method that checks for judgment presence and pass status without
	 * requiring direct access to the Judgment object.
	 * </p>
	 * @return true if judgment exists and passed, false otherwise
	 */
	public boolean isJudgmentPassed() {
		Boolean pass = (Boolean) this.context.get("judgment.pass");
		return pass != null && pass;
	}

	/**
	 * Get the verdict from jury evaluation.
	 * <p>
	 * Retrieves the verdict object stored by
	 * {@link org.springaicommunity.agents.advisors.judge.JuryAdvisor} after jury
	 * evaluation completes. The verdict contains the aggregated judgment and all
	 * individual judgments from the jury.
	 * </p>
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>{@code
	 * AgentClientResponse response = agentClient
	 *     .advisors(JuryAdvisor.builder().jury(jury).build())
	 *     .run();
	 *
	 * Verdict verdict = response.getVerdict();
	 * if (verdict != null && verdict.aggregated().pass()) {
	 *     // Task passed jury evaluation
	 * }
	 * }</pre>
	 * @param <T> the verdict type (typically Verdict from spring-ai-agents-judge)
	 * @return the verdict result, or null if no jury evaluation was performed
	 */
	@SuppressWarnings("unchecked")
	public <T> T getVerdict() {
		return (T) this.context.get("verdict");
	}

	/**
	 * Check if a verdict was rendered and the aggregated judgment passed.
	 * <p>
	 * Convenience method that checks for verdict presence and aggregated pass status
	 * without requiring direct access to the Verdict object.
	 * </p>
	 * @return true if verdict exists and aggregated judgment passed, false otherwise
	 */
	public boolean isVerdictPassed() {
		Boolean pass = (Boolean) this.context.get("verdict.pass");
		return pass != null && pass;
	}

}