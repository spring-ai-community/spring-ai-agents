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
import org.springaicommunity.agents.judge.result.JudgmentStatus;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springaicommunity.agents.judge.score.NumericalScore;
import org.springaicommunity.agents.judge.score.Score;

import java.util.List;
import java.util.Map;

/**
 * Median voting strategy for numerical judgments.
 *
 * <p>
 * Computes the median of all judgment scores, which is robust to outliers. Boolean scores
 * are converted to numerical (true=1.0, false=0.0) before computing the median.
 * </p>
 *
 * <p>
 * For an even number of judgments, the median is the average of the two middle values.
 * </p>
 *
 * <p>
 * The judgment passes if the median score is >= 0.5.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * VotingStrategy strategy = new MedianVotingStrategy();
 * Judgment result = strategy.aggregate(judgments, Map.of());
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class MedianVotingStrategy implements VotingStrategy {

	private static final double THRESHOLD = 0.5;

	@Override
	public Judgment aggregate(List<Judgment> judgments, Map<String, Double> weights) {
		if (judgments == null || judgments.isEmpty()) {
			throw new IllegalArgumentException("Cannot aggregate empty judgment list");
		}

		List<Double> scores = judgments.stream().map(j -> toNumerical(j.score())).sorted().toList();

		double median;
		int size = scores.size();

		if (size % 2 == 0) {
			// Even number: average of two middle values
			median = (scores.get(size / 2 - 1) + scores.get(size / 2)) / 2.0;
		}
		else {
			// Odd number: middle value
			median = scores.get(size / 2);
		}

		boolean pass = median >= THRESHOLD;

		String reasoning = String.format("Median score: %.2f (threshold: %.2f, result: %s)", median, THRESHOLD,
				pass ? "pass" : "fail");

		return Judgment.builder()
			.score(new NumericalScore(median, 0.0, 1.0))
			.status(pass ? JudgmentStatus.PASS : JudgmentStatus.FAIL)
			.reasoning(reasoning)
			.build();
	}

	@Override
	public String getName() {
		return "MedianVoting";
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
