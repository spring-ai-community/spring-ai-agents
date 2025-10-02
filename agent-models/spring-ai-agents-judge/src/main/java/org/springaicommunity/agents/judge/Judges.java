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

import java.util.Optional;

import org.springaicommunity.agents.judge.result.Judgment;

/**
 * Utility class for creating and composing judges.
 *
 * <p>
 * Provides factory methods for common judge operations:
 * <ul>
 * <li>Wrapping lambda judges with metadata via {@link NamedJudge}</li>
 * <li>Creating simple pass/fail judges</li>
 * <li>Extracting metadata from judges</li>
 * </ul>
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Wrap lambda with metadata
 * Judge fileCheck = ctx -> Judgment.pass("File exists");
 * NamedJudge named = Judges.named(fileCheck, "FileCheck", "Checks if file exists");
 *
 * // Simple judges
 * Judge alwaysPass = Judges.alwaysPass("Default success");
 * Judge alwaysFail = Judges.alwaysFail("Not implemented");
 *
 * // Extract metadata
 * Optional<JudgeMetadata> meta = Judges.tryMetadata(judge);
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public final class Judges {

	private Judges() {
		// Utility class
	}

	/**
	 * Wrap a judge with a custom name.
	 * <p>
	 * Useful for lambda judges that need identifiable names for logging, monitoring, or
	 * display purposes.
	 * </p>
	 * @param judge the judge to wrap
	 * @param name the judge name
	 * @return named judge with metadata
	 */
	public static NamedJudge named(Judge judge, String name) {
		return named(judge, name, null, JudgeType.DETERMINISTIC);
	}

	/**
	 * Wrap a judge with name and description.
	 * @param judge the judge to wrap
	 * @param name the judge name
	 * @param description the judge description
	 * @return named judge with metadata
	 */
	public static NamedJudge named(Judge judge, String name, String description) {
		return named(judge, name, description, JudgeType.DETERMINISTIC);
	}

	/**
	 * Wrap a judge with complete metadata.
	 * @param judge the judge to wrap
	 * @param name the judge name
	 * @param description the judge description
	 * @param type the judge type
	 * @return named judge with metadata
	 */
	public static NamedJudge named(Judge judge, String name, String description, JudgeType type) {
		return new NamedJudge(judge, new JudgeMetadata(name, description, type));
	}

	/**
	 * Create a judge that always passes with the given reasoning.
	 * @param reasoning the reasoning to include in judgment
	 * @return judge that always passes
	 */
	public static Judge alwaysPass(String reasoning) {
		return ctx -> Judgment.pass(reasoning);
	}

	/**
	 * Create a judge that always fails with the given reasoning.
	 * @param reasoning the reasoning to include in judgment
	 * @return judge that always fails
	 */
	public static Judge alwaysFail(String reasoning) {
		return ctx -> Judgment.fail(reasoning);
	}

	/**
	 * Attempt to extract metadata from a judge.
	 * <p>
	 * Returns metadata if the judge implements {@link JudgeWithMetadata}, otherwise
	 * returns empty. This uses Spring's marker interface pattern for semantic type
	 * checking. Useful for infrastructure code that needs to log or display judge
	 * information.
	 * </p>
	 * @param judge the judge to extract metadata from
	 * @return metadata if available, otherwise empty
	 */
	public static Optional<JudgeMetadata> tryMetadata(Judge judge) {
		return (judge instanceof JudgeWithMetadata jwm) ? Optional.of(jwm.metadata()) : Optional.empty();
	}

	/**
	 * Compose two judges with AND logic.
	 * <p>
	 * Returns a judge that executes the first judge, and only if it passes, executes the
	 * second judge. If the first fails, its judgment is returned immediately
	 * (short-circuit evaluation). This is analogous to Spring Security's CompositeVoter
	 * or JUnit's RuleChain pattern.
	 * </p>
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>{@code
	 * Judge validation = Judges.and(fileExists, contentMatches);
	 * Judge chained = Judges.and(fileExists, Judges.and(contentMatches, buildSucceeds));
	 * }</pre>
	 * @param first the first judge to execute
	 * @param second the second judge to execute (only if first passes)
	 * @return composed judge with AND logic
	 */
	public static Judge and(Judge first, Judge second) {
		return ctx -> {
			Judgment firstResult = first.judge(ctx);
			return firstResult.pass() ? second.judge(ctx) : firstResult;
		};
	}

	/**
	 * Compose two judges with OR logic.
	 * <p>
	 * Returns a judge that executes the first judge, and only if it fails, executes the
	 * second judge. If the first passes, its judgment is returned immediately
	 * (short-circuit evaluation).
	 * </p>
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>{@code
	 * Judge fallback = Judges.or(primaryCheck, secondaryCheck);
	 * }</pre>
	 * @param first the first judge to execute
	 * @param second the second judge to execute (only if first fails)
	 * @return composed judge with OR logic
	 */
	public static Judge or(Judge first, Judge second) {
		return ctx -> {
			Judgment firstResult = first.judge(ctx);
			return firstResult.pass() ? firstResult : second.judge(ctx);
		};
	}

	/**
	 * Compose multiple judges with AND logic (all must pass).
	 * <p>
	 * Returns a judge that executes all judges in sequence. If any judge fails, its
	 * judgment is returned immediately (short-circuit evaluation). If all judges pass, a
	 * passing judgment is returned. This is analogous to Stream.allMatch().
	 * </p>
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>{@code
	 * Judge validation = Judges.allOf(fileExists, contentMatches, buildSucceeds, testsPass);
	 * }</pre>
	 * @param judges the judges to compose (varargs)
	 * @return composed judge with AND logic
	 */
	public static Judge allOf(Judge... judges) {
		return ctx -> {
			for (Judge judge : judges) {
				Judgment judgment = judge.judge(ctx);
				if (!judgment.pass()) {
					return judgment;
				}
			}
			return Judgment.pass("All checks passed");
		};
	}

	/**
	 * Compose multiple judges with OR logic (any must pass).
	 * <p>
	 * Returns a judge that executes all judges in sequence. If any judge passes, its
	 * judgment is returned immediately (short-circuit evaluation). If all judges fail, a
	 * failing judgment is returned. This is analogous to Stream.anyMatch().
	 * </p>
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>{@code
	 * Judge fallback = Judges.anyOf(checkA, checkB, checkC);
	 * }</pre>
	 * @param judges the judges to compose (varargs)
	 * @return composed judge with OR logic
	 */
	public static Judge anyOf(Judge... judges) {
		return ctx -> {
			for (Judge judge : judges) {
				Judgment judgment = judge.judge(ctx);
				if (judgment.pass()) {
					return judgment;
				}
			}
			return Judgment.fail("All checks failed");
		};
	}

}
