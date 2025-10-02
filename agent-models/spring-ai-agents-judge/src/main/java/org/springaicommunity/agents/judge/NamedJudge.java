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
 * Wrapper that adds metadata to a Judge through composition.
 *
 * <p>
 * This class implements the composition-over-inheritance pattern to attach metadata
 * (name, description, type) to any Judge implementation without polluting the core Judge
 * interface with default methods. This preserves functional interface purity while
 * enabling rich metadata when needed for logging, monitoring, or display.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Wrap a lambda judge with metadata
 * Judge simple = ctx -> Judgment.pass("Success");
 * NamedJudge named = new NamedJudge(
 *     simple,
 *     new JudgeMetadata("SimpleCheck", "Basic validation", JudgeType.DETERMINISTIC)
 * );
 *
 * // Access metadata
 * log.info("Running judge: {}", named.metadata().name());
 *
 * // Use as normal Judge
 * Judgment result = named.judge(context);
 *
 * // Pattern matching for metadata extraction
 * if (judge instanceof NamedJudge nj) {
 *     JudgeMetadata meta = nj.metadata();
 *     // ... use metadata
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see Judge
 * @see JudgeMetadata
 * @see Judges
 */
public final class NamedJudge implements Judge {

	private final Judge delegate;

	private final JudgeMetadata metadata;

	/**
	 * Create a named judge wrapping the given delegate with metadata.
	 * @param delegate the judge to wrap
	 * @param metadata the metadata for this judge
	 */
	public NamedJudge(Judge delegate, JudgeMetadata metadata) {
		this.delegate = delegate;
		this.metadata = metadata;
	}

	@Override
	public Judgment judge(JudgmentContext context) {
		return this.delegate.judge(context);
	}

	/**
	 * Get the metadata for this judge.
	 * @return the judge metadata
	 */
	public JudgeMetadata metadata() {
		return this.metadata;
	}

}
