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

package org.springaicommunity.agents.judge.agent;

import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.judge.JudgeMetadata;
import org.springaicommunity.judge.JudgeType;
import org.springaicommunity.judge.JudgeWithMetadata;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;
import org.springaicommunity.judge.score.BooleanScore;
import org.springaicommunity.judge.score.NumericalScore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Judge that delegates evaluation to an agent.
 *
 * <p>
 * Uses another agent to judge agent execution results. This enables sophisticated
 * evaluation by leveraging agent capabilities (code analysis, security audits, etc.).
 * </p>
 *
 * <p>
 * The agent receives a structured prompt with the original goal, agent output, and
 * evaluation criteria. The agent's response is parsed for:
 * </p>
 * <ul>
 * <li>PASS: true/false</li>
 * <li>SCORE: numerical value (optional)</li>
 * <li>REASONING: explanation</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * AgentJudge judge = AgentJudge.builder()
 *     .agentClient(agentClient)
 *     .criteria("Evaluate code quality and best practices")
 *     .build();
 *
 * Judgment result = judge.judge(context);
 * }</pre>
 *
 * <p>
 * Pre-configured factory methods:
 * </p>
 * <pre>{@code
 * AgentJudge codeReview = AgentJudge.codeReview(agentClient);
 * AgentJudge securityAudit = AgentJudge.securityAudit(agentClient);
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class AgentJudge implements JudgeWithMetadata {

	private static final Pattern PASS_PATTERN = Pattern.compile("PASS:\\s*(true|false)", Pattern.CASE_INSENSITIVE);

	private static final Pattern SCORE_PATTERN = Pattern.compile("SCORE:\\s*([0-9.]+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern REASONING_PATTERN = Pattern.compile("REASONING:\\s*(.+)",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private final JudgeMetadata metadata;

	private final AgentClient agentClient;

	private final String criteria;

	private final String goalTemplate;

	private AgentJudge(String name, String description, AgentClient agentClient, String criteria, String goalTemplate) {
		this.metadata = new JudgeMetadata(name, description, JudgeType.AGENT);
		this.agentClient = agentClient;
		this.criteria = criteria;
		this.goalTemplate = goalTemplate != null ? goalTemplate : buildDefaultGoalTemplate();
	}

	@Override
	public JudgeMetadata metadata() {
		return metadata;
	}

	@Override
	public Judgment judge(JudgmentContext context) {
		// Format goal from template
		String goal = formatGoal(context);

		// Execute agent
		AgentClientResponse response = agentClient.goal(goal).workingDirectory(context.workspace()).run();

		// Parse agent response
		return parseAgentResponse(response.getResult(), context);
	}

	/**
	 * Format the judge goal from template and context.
	 * @param context the judgment context
	 * @return formatted goal
	 */
	private String formatGoal(JudgmentContext context) {
		String goal = context.goal();
		String workspace = context.workspace() != null ? context.workspace().toString() : "Not specified";
		String output = context.agentOutput().map(Object::toString).orElse("No output provided");
		String status = context.status().toString();

		return goalTemplate.replace("{goal}", goal)
			.replace("{workspace}", workspace)
			.replace("{output}", output)
			.replace("{status}", status)
			.replace("{criteria}", criteria);
	}

	/**
	 * Parse the agent's response into a Judgment.
	 * @param agentOutput the agent's output
	 * @param context the judgment context
	 * @return parsed judgment
	 */
	private Judgment parseAgentResponse(String agentOutput, JudgmentContext context) {
		// Extract PASS
		boolean pass = extractPass(agentOutput);

		// Extract SCORE (optional)
		Double score = extractScore(agentOutput);

		// Extract REASONING
		String reasoning = extractReasoning(agentOutput);

		Judgment.Builder builder = Judgment.builder()
			.status(pass ? JudgmentStatus.PASS : JudgmentStatus.FAIL)
			.reasoning(reasoning);

		if (score != null) {
			// Use numerical score if provided
			builder.score(new NumericalScore(score, 0.0, 10.0));
		}
		else {
			// Fallback to boolean score
			builder.score(new BooleanScore(pass));
		}

		return builder.build();
	}

	private boolean extractPass(String output) {
		Matcher matcher = PASS_PATTERN.matcher(output);
		if (matcher.find()) {
			return Boolean.parseBoolean(matcher.group(1));
		}
		// Default to false if not found
		return false;
	}

	private Double extractScore(String output) {
		Matcher matcher = SCORE_PATTERN.matcher(output);
		if (matcher.find()) {
			try {
				return Double.parseDouble(matcher.group(1));
			}
			catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private String extractReasoning(String output) {
		Matcher matcher = REASONING_PATTERN.matcher(output);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		// Fallback: return entire output as reasoning
		return output;
	}

	private String buildDefaultGoalTemplate() {
		return """
				Evaluate the following agent execution:

				Original Goal: {goal}
				Workspace: {workspace}
				Agent Output: {output}
				Execution Status: {status}

				Evaluation Criteria:
				{criteria}

				Provide your judgment in the following format:
				PASS: true/false
				SCORE: X.X (0-10, optional)
				REASONING: Your detailed explanation

				Be thorough and specific in your reasoning.
				""";
	}

	/**
	 * Create a code review agent judge.
	 * @param agentClient the agent client
	 * @return code review judge
	 */
	public static AgentJudge codeReview(AgentClient agentClient) {
		return builder().agentClient(agentClient)
			.name("CodeReview")
			.description("Agent-powered code review")
			.criteria("Review code quality, correctness, best practices, and potential bugs")
			.build();
	}

	/**
	 * Create a security audit agent judge.
	 * @param agentClient the agent client
	 * @return security audit judge
	 */
	public static AgentJudge securityAudit(AgentClient agentClient) {
		return builder().agentClient(agentClient)
			.name("SecurityAudit")
			.description("Agent-powered security audit")
			.criteria("Identify security vulnerabilities, risks, and compliance issues")
			.build();
	}

	/**
	 * Create a new builder.
	 * @return builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for AgentJudge.
	 */
	public static class Builder {

		private String name = "AgentJudge";

		private String description = "Agent-powered evaluation";

		private AgentClient agentClient;

		private String criteria;

		private String goalTemplate;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder agentClient(AgentClient agentClient) {
			this.agentClient = agentClient;
			return this;
		}

		public Builder criteria(String criteria) {
			this.criteria = criteria;
			return this;
		}

		public Builder goalTemplate(String goalTemplate) {
			this.goalTemplate = goalTemplate;
			return this;
		}

		public AgentJudge build() {
			if (agentClient == null) {
				throw new IllegalStateException("AgentClient is required");
			}
			if (criteria == null) {
				throw new IllegalStateException("Criteria is required");
			}
			return new AgentJudge(name, description, agentClient, criteria, goalTemplate);
		}

	}

}
