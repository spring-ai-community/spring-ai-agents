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

package org.springaicommunity.agents.judge.impl;

import org.springaicommunity.agents.judge.JudgeMetadata;
import org.springaicommunity.agents.judge.JudgeType;
import org.springaicommunity.agents.judge.JudgeWithMetadata;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Base class for LLM-powered judges.
 *
 * <p>
 * LLM judges use language models to evaluate agent execution results. This abstract class
 * provides a template method pattern where subclasses customize prompt construction and
 * response parsing.
 * </p>
 *
 * <p>
 * <strong>Template Method Pattern:</strong> The {@link #judge(JudgmentContext)} method
 * orchestrates the evaluation flow: build prompt → call LLM → parse response. Subclasses
 * implement {@link #buildPrompt(JudgmentContext)} and
 * {@link #parseResponse(String, JudgmentContext)} to customize behavior.
 * </p>
 *
 * <p>
 * <strong>Design Rationale:</strong> LLM judges complement deterministic judges by
 * providing nuanced evaluation that's difficult to express in rules. Examples: code
 * quality assessment, semantic correctness, creativity evaluation. While slower and more
 * expensive than deterministic judges, they excel at subjective or complex criteria.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * public class CodeQualityJudge extends LLMJudge {
 *
 *     public CodeQualityJudge(ChatClient.Builder chatClientBuilder) {
 *         super("CodeQuality", "Evaluates code quality 0-10", chatClientBuilder);
 *     }
 *
 *     &#64;Override
 *     protected String buildPrompt(JudgmentContext context) {
 *         return String.format("""
 *             Review this code and rate quality 0-10:
 *             %s
 *             """, context.agentOutput().orElse(""));
 *     }
 *
 *

&#64;Override
 *     protected Judgment parseResponse(String response, JudgmentContext context) {
 *         // Extract score and reasoning from LLM response
 *         return Judgment.builder()
 *             .score(new NumericalScore(extractScore(response)))
 *             .reasoning(response)
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public abstract class LLMJudge implements JudgeWithMetadata {

	private final JudgeMetadata metadata;

	protected final ChatClient chatClient;

	/**
	 * Create an LLM judge with metadata and chat client.
	 * @param name the judge name
	 * @param description the judge description
	 * @param chatClientBuilder the chat client builder for LLM calls (null allowed for
	 * testing)
	 */
	protected LLMJudge(String name, String description, ChatClient.Builder chatClientBuilder) {
		this.metadata = new JudgeMetadata(name, description, JudgeType.LLM_POWERED);
		this.chatClient = chatClientBuilder != null ? chatClientBuilder.build() : null;
	}

	/**
	 * Build the prompt to send to the LLM.
	 * <p>
	 * Subclasses implement this to construct prompts from judgment context. Include goal,
	 * workspace, agent output, and any other relevant context. Use clear instructions for
	 * the LLM to follow.
	 * </p>
	 * @param context the judgment context
	 * @return the prompt string
	 */
	protected abstract String buildPrompt(JudgmentContext context);

	/**
	 * Parse the LLM response into a judgment.
	 * <p>
	 * Subclasses implement this to extract score, pass/fail, and reasoning from the LLM's
	 * text response. Handle edge cases like unclear responses, missing data, or
	 * unexpected formats.
	 * </p>
	 * @param response the LLM response text
	 * @param context the original judgment context
	 * @return the parsed judgment
	 */
	protected abstract Judgment parseResponse(String response, JudgmentContext context);

	/**
	 * Evaluate the agent execution using the LLM.
	 * <p>
	 * Template method that orchestrates: build prompt → call LLM → parse response.
	 * Subclasses customize via {@link #buildPrompt} and {@link #parseResponse}.
	 * </p>
	 * @param context the judgment context
	 * @return the judgment from the LLM
	 */
	@Override
	public Judgment judge(JudgmentContext context) {
		String prompt = buildPrompt(context);
		String response = this.chatClient.prompt().user(prompt).call().content();
		return parseResponse(response, context);
	}

	@Override
	public JudgeMetadata metadata() {
		return this.metadata;
	}

}
