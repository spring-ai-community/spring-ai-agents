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
	 * Returns metadata if the judge is a {@link NamedJudge}, otherwise returns empty.
	 * Useful for infrastructure code that needs to log or display judge information.
	 * </p>
	 * @param judge the judge to extract metadata from
	 * @return metadata if available, otherwise empty
	 */
	public static Optional<JudgeMetadata> tryMetadata(Judge judge) {
		return (judge instanceof NamedJudge nj) ? Optional.of(nj.metadata()) : Optional.empty();
	}

}
