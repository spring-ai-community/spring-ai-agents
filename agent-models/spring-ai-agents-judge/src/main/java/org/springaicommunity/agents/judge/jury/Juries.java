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

package org.springaicommunity.agents.judge.jury;

import org.springaicommunity.agents.judge.Judge;
import org.springaicommunity.agents.judge.JudgeType;
import org.springaicommunity.agents.judge.Judges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for jury composition and transformation.
 *
 * <p>
 * Provides factory methods and composition utilities for creating juries from judges,
 * combining multiple juries, and building meta-juries (juries of juries).
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Create jury from judges with auto-naming
 * Jury jury = Juries.fromJudges(
 *     new MajorityVotingStrategy(),
 *     fileJudge, buildJudge, correctnessJudge
 * );
 *
 * // Combine two juries
 * Jury combined = Juries.combine(jury1, jury2, new WeightedAverageStrategy());
 *
 * // Create meta-jury (jury of juries)
 * Jury metaJury = Juries.allOf(new ConsensusStrategy(), jury1, jury2, jury3);
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see Jury
 * @see SimpleJury
 * @see MetaJury
 */
public final class Juries {

	private Juries() {
		// Utility class - no instantiation
	}

	/**
	 * Create a jury from judges with automatic naming and unique identity preservation.
	 *
	 * <p>
	 * Judges without metadata are auto-named as "Judge#1", "Judge#2", etc. If duplicate
	 * names are detected, suffixes "-2", "-3", etc. are added deterministically to ensure
	 * uniqueness.
	 * </p>
	 * @param strategy the voting strategy
	 * @param judges the judges to include
	 * @return a simple jury with named judges
	 */
	public static Jury fromJudges(VotingStrategy strategy, Judge... judges) {
		if (judges == null || judges.length == 0) {
			throw new IllegalArgumentException("At least one judge is required");
		}

		SimpleJury.Builder builder = SimpleJury.builder().votingStrategy(strategy);

		Map<String, Integer> nameCount = new HashMap<>();

		for (int i = 0; i < judges.length; i++) {
			Judge judge = judges[i];
			String baseName = Judges.tryMetadata(judge).map(m -> m.name()).orElse("Judge#" + (i + 1));

			// Handle duplicate names with suffix
			String uniqueName = baseName;
			if (nameCount.containsKey(baseName)) {
				int count = nameCount.get(baseName) + 1;
				nameCount.put(baseName, count);
				uniqueName = baseName + "-" + count;
			}
			else {
				nameCount.put(baseName, 1);
			}

			// Wrap with unique name if needed
			if (!uniqueName.equals(baseName)) {
				judge = Judges.named(judge, uniqueName, null, JudgeType.DETERMINISTIC);
			}

			builder.judge(judge);
		}

		return builder.build();
	}

	/**
	 * Combine two juries into a meta-jury.
	 * @param first the first jury
	 * @param second the second jury
	 * @param metaStrategy the voting strategy for aggregating jury verdicts
	 * @return a meta-jury combining both juries
	 */
	public static Jury combine(Jury first, Jury second, VotingStrategy metaStrategy) {
		if (first == null || second == null) {
			throw new IllegalArgumentException("Both juries must be non-null");
		}
		return new MetaJury(Arrays.asList(first, second), metaStrategy);
	}

	/**
	 * Create a meta-jury from multiple juries.
	 * @param strategy the voting strategy for aggregating jury verdicts
	 * @param juries the juries to combine
	 * @return a meta-jury combining all juries
	 */
	public static Jury allOf(VotingStrategy strategy, Jury... juries) {
		if (juries == null || juries.length == 0) {
			throw new IllegalArgumentException("At least one jury is required");
		}
		return new MetaJury(Arrays.asList(juries), strategy);
	}

}
