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

package org.springaicommunity.agents.judge.score;

import java.util.Map;

/**
 * Utility class for score conversions and normalizations.
 *
 * <p>
 * Provides helpers for converting different score types to normalized [0.0, 1.0] range,
 * which is essential for voting strategies that need to compare heterogeneous scores.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public final class Scores {

	private Scores() {
		// Utility class - no instantiation
	}

	/**
	 * Convert any score type to normalized [0.0, 1.0] range.
	 *
	 * <p>
	 * Conversion rules:
	 * </p>
	 * <ul>
	 * <li>null → 0.0 (handles ABSTAIN/ERROR cases with no score)</li>
	 * <li>BooleanScore → 1.0 (true) or 0.0 (false)</li>
	 * <li>NumericalScore → normalized() value</li>
	 * <li>CategoricalScore → map lookup (default 0.0)</li>
	 * </ul>
	 * @param score the score to normalize (can be null)
	 * @param categoryMap mapping from categorical values to normalized scores (empty for
	 * non-categorical)
	 * @return normalized score between 0.0 and 1.0
	 */
	public static double toNormalized(Score score, Map<String, Double> categoryMap) {
		if (score == null) {
			return 0.0;
		}

		if (score instanceof BooleanScore bs) {
			return bs.value() ? 1.0 : 0.0;
		}

		if (score instanceof NumericalScore ns) {
			return ns.normalized();
		}

		if (score instanceof CategoricalScore cs) {
			return categoryMap.getOrDefault(cs.value(), 0.0);
		}

		throw new IllegalArgumentException("Unknown score type: " + score.getClass().getName());
	}

}
