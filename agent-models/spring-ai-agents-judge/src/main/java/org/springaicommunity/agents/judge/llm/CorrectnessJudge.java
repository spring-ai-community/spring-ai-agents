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

package org.springaicommunity.agents.judge.llm;

import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.result.JudgmentStatus;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springframework.ai.chat.client.ChatClient;

/**
 * LLM-powered judge that evaluates if the agent accomplished its goal.
 *
 * <p>
 * CorrectnessJudge uses an LLM to determine if the agent successfully completed the task
 * specified in the goal. It provides a simple YES/NO judgment with reasoning, making it
 * ideal for cases where semantic understanding is needed but deterministic rules are
 * insufficient.
 * </p>
 *
 * <p>
 * <strong>When to Use:</strong>
 * </p>
 * <ul>
 * <li>Goal requires semantic interpretation (e.g., "write helpful documentation")</li>
 * <li>Success criteria are subjective or nuanced</li>
 * <li>Deterministic judges (file checks, command output) don't capture full success</li>
 * <li>Need reasoning explanation for why task succeeded/failed</li>
 * </ul>
 *
 * <p>
 * <strong>Trade-offs:</strong>
 * </p>
 * <ul>
 * <li>✅ Handles subjective/semantic criteria</li>
 * <li>✅ Provides natural language reasoning</li>
 * <li>❌ Slower than deterministic judges (LLM call latency)</li>
 * <li>❌ More expensive (API costs)</li>
 * <li>❌ Non-deterministic (same input may yield different outputs)</li>
 * </ul>
 *
 * <p>
 * <strong>Best Practice:</strong> Combine with deterministic judges in a Jury for robust
 * evaluation. Use deterministic judges for objective criteria (file exists, build
 * succeeds) and CorrectnessJudge for subjective assessment (quality, helpfulness).
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Standalone evaluation
 * Judge judge = new CorrectnessJudge(chatClientBuilder);
 * Judgment judgment = judge.judge(context);
 * System.out.println(judgment.reasoning());
 *
 * // Combined with AgentClient via JudgeAdvisor
 * AgentClientResponse response = agentClient
 *     .goal("Write a README with installation instructions")
 *     .advisors(JudgeAdvisor.builder()
 *         .judge(new CorrectnessJudge(chatClientBuilder))
 *         .build())
 *     .run();
 *
 * if (response.isJudgmentPassed()) {
 *     System.out.println("Task succeeded!");
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class CorrectnessJudge extends LLMJudge {

	/**
	 * Create a correctness judge with the given chat client builder.
	 * @param chatClientBuilder the chat client builder for LLM calls
	 */
	public CorrectnessJudge(ChatClient.Builder chatClientBuilder) {
		super("Correctness", "Evaluates if agent accomplished the goal", chatClientBuilder);
	}

	@Override
	protected String buildPrompt(JudgmentContext context) {
		String goal = context.goal();
		String workspace = context.workspace() != null ? context.workspace().toString() : "Not specified";
		String output = context.agentOutput().orElse("No output provided");

		return String.format("""
				Goal: %s
				Workspace: %s
				Agent Output: %s

				Did the agent accomplish the goal? Answer YES or NO, followed by your reasoning.

				Format your response as:
				Answer: [YES or NO]
				Reasoning: [Your explanation]
				""", goal, workspace, output);
	}

	@Override
	protected Judgment parseResponse(String response, JudgmentContext context) {
		// Extract YES/NO answer
		boolean pass = response.toUpperCase().contains("YES");

		// Extract reasoning (everything after "Reasoning:")
		String reasoning = extractReasoning(response);

		return Judgment.builder()
			.score(new BooleanScore(pass))
			.status(pass ? JudgmentStatus.PASS : JudgmentStatus.FAIL)
			.reasoning(reasoning)
			.build();
	}

	private String extractReasoning(String response) {
		// Try to extract reasoning section
		int reasoningIndex = response.indexOf("Reasoning:");
		if (reasoningIndex >= 0) {
			return response.substring(reasoningIndex + "Reasoning:".length()).trim();
		}

		// Fallback: try to extract everything after YES/NO line
		String[] lines = response.split("\n");
		StringBuilder reasoning = new StringBuilder();
		boolean foundAnswer = false;

		for (String line : lines) {
			if (line.toUpperCase().contains("YES") || line.toUpperCase().contains("NO")) {
				foundAnswer = true;
				continue;
			}
			if (foundAnswer && !line.trim().isEmpty()) {
				if (!reasoning.isEmpty()) {
					reasoning.append(" ");
				}
				reasoning.append(line.trim());
			}
		}

		if (!reasoning.isEmpty()) {
			return reasoning.toString();
		}

		// Final fallback: return full response
		return response;
	}

}
