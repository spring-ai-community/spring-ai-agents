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

package org.springaicommunity.agents.judge;

import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;

/**
 * Core interface for judging agent execution results.
 *
 * <p>
 * Judges evaluate whether an agent successfully accomplished its goal. They can be
 * deterministic (rule-based) or AI-powered (LLM-based). This interface defines the
 * synchronous evaluation contract.
 * </p>
 *
 * <p>
 * Judges are designed to be used as Spring beans and configured via dependency injection
 * rather than factory methods. For asynchronous evaluation, see {@link AsyncJudge}.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * &#64;Component
 * public class FileExistsJudge implements Judge {
 *     public Judgment judge(JudgmentContext context) {
 *         Path file = context.workspace().resolve(targetPath);
 *         boolean exists = Files.exists(file);
 *         return Judgment.builder()
 *             .score(new BooleanScore(exists))
 *             .pass(exists)
 *             .reasoning(exists ? "File exists" : "File not found")
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see AsyncJudge
 * @see ReactiveJudge
 */
public interface Judge {

	/**
	 * Evaluate an agent execution result.
	 * @param context the judgment context containing all information about the agent
	 * execution
	 * @return the judgment with score, pass/fail, reasoning, and checks
	 */
	Judgment judge(JudgmentContext context);

	/**
	 * Get metadata about this judge (name, description, type).
	 * @return judge metadata
	 */
	JudgeMetadata getMetadata();

}
