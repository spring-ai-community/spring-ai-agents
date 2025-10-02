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
 * Pure functional interface for judging agent execution results.
 *
 * <p>
 * This interface defines the core judging contract as a single abstract method, enabling
 * functional programming patterns like lambdas, method references, and composition.
 * </p>
 *
 * <p>
 * <strong>Functional Purity:</strong> Judge is intentionally minimal - a single method
 * with no default methods or metadata concerns. This preserves functional interface
 * discipline and clean separation of concerns. For judges that need metadata (name,
 * description, type), use {@link NamedJudge} which wraps a Judge with metadata through
 * composition.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Pure lambda judges
 * Judge simpleCheck = ctx -> Judgment.pass("Success");
 *
 * // Method references
 * Judge validator = this::validateOutput;
 *
 * // Composition
 * Judge combined = ctx -> {
 *     Judgment first = checkOne.judge(ctx);
 *     return first.pass() ? checkTwo.judge(ctx) : first;
 * };
 *
 * // With metadata via NamedJudge
 * NamedJudge named = Judges.named(simpleCheck, "SimpleCheck", "Basic validation", JudgeType.DETERMINISTIC);
 * }</pre>
 *
 * <p>
 * <strong>Design Inspiration:</strong> This interface draws from the "judges" framework's
 * clean BaseJudge abstraction and Spring AI's Evaluator pattern - a single abstract
 * method with rich context. The composition-over-inheritance approach (NamedJudge
 * wrapper) avoids default method pollution while maintaining functional purity.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see NamedJudge
 * @see Judges
 */
@FunctionalInterface
public interface Judge {

	/**
	 * Evaluate an agent execution result.
	 * @param context the judgment context containing all information about the agent
	 * execution
	 * @return the judgment with score, pass/fail, reasoning, and checks
	 */
	Judgment judge(JudgmentContext context);

}
