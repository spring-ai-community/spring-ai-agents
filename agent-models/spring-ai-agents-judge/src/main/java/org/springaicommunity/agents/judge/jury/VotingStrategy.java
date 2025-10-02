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

import org.springaicommunity.agents.judge.result.Judgment;

import java.util.List;
import java.util.Map;

/**
 * Strategy for aggregating multiple judgments into a single verdict.
 *
 * <p>
 * VotingStrategy implementations define how to combine individual judgments from multiple
 * judges into a single aggregated judgment. Different strategies support different
 * scoring types (boolean, numerical, categorical) and aggregation methods (majority,
 * average, weighted, consensus).
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * VotingStrategy strategy = new MajorityVotingStrategy();
 * Judgment aggregated = strategy.aggregate(judgments, weights);
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see MajorityVotingStrategy
 * @see AverageVotingStrategy
 * @see WeightedAverageStrategy
 */
public interface VotingStrategy {

	/**
	 * Aggregate multiple judgments into a single judgment.
	 * @param judgments the list of individual judgments from judges
	 * @param weights optional weights for each judge (empty map for equal weights)
	 * @return aggregated judgment
	 */
	Judgment aggregate(List<Judgment> judgments, Map<String, Double> weights);

	/**
	 * Get the name of this voting strategy (for debugging and metadata).
	 * @return strategy name
	 */
	String getName();

}
