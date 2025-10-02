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
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springaicommunity.agents.judge.score.NumericalScore;
import org.springaicommunity.agents.judge.score.Score;

import java.util.List;
import java.util.Map;

/**
 * Weighted average voting strategy for numerical judgments.
 *
 * <p>
 * Computes a weighted average of judgment scores using the provided weights map. If no
 * weights are provided, falls back to equal weights (simple average). Boolean scores are
 * converted to numerical (true=1.0, false=0.0) before weighting.
 * </p>
 *
 * <p>
 * Weights are indexed by judge position (0, 1, 2, ...) as strings in the map. Weights do
 * not need to sum to 1.0 - they will be normalized automatically.
 * </p>
 *
 * <p>
 * The judgment passes if the weighted average is >= 0.5.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * Map<String, Double> weights = Map.of("0", 0.3, "1", 0.7);
 * VotingStrategy strategy = new WeightedAverageStrategy();
 * Judgment result = strategy.aggregate(judgments, weights);
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class WeightedAverageStrategy implements VotingStrategy {

	private static final double THRESHOLD = 0.5;

	@Override
	public Judgment aggregate(List<Judgment> judgments, Map<String, Double> weights) {
		if (judgments == null || judgments.isEmpty()) {
			throw new IllegalArgumentException("Cannot aggregate empty judgment list");
		}

		// If no weights provided, use equal weights
		if (weights == null || weights.isEmpty()) {
			return new AverageVotingStrategy().aggregate(judgments, weights);
		}

		double weightedSum = 0.0;
		double weightSum = 0.0;

		for (int i = 0; i < judgments.size(); i++) {
			String key = String.valueOf(i);
			double weight = weights.getOrDefault(key, 1.0);
			double score = toNumerical(judgments.get(i).score());

			weightedSum += score * weight;
			weightSum += weight;
		}

		double weightedAverage = weightedSum / weightSum;

		boolean pass = weightedAverage >= THRESHOLD;

		String reasoning = String.format("Weighted average: %.2f (threshold: %.2f, result: %s)", weightedAverage,
				THRESHOLD, pass ? "pass" : "fail");

		return Judgment.builder()
			.score(new NumericalScore(weightedAverage, 0.0, 1.0))
			.pass(pass)
			.reasoning(reasoning)
			.build();
	}

	@Override
	public String getName() {
		return "WeightedAverage";
	}

	/**
	 * Convert any score type to numerical [0.0, 1.0].
	 * @param score the score to convert
	 * @return numerical value
	 */
	private double toNumerical(Score score) {
		if (score instanceof BooleanScore bs) {
			return bs.value() ? 1.0 : 0.0;
		}
		else if (score instanceof NumericalScore ns) {
			return ns.normalized();
		}
		return 0.0;
	}

}
