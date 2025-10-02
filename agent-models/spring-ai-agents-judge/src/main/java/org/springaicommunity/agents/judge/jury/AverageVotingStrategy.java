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
 * Average voting strategy for numerical judgments.
 *
 * <p>
 * Computes the simple average of all judgment scores. Boolean scores are converted to
 * numerical (true=1.0, false=0.0) before averaging. The result is normalized to [0.0,
 * 1.0].
 * </p>
 *
 * <p>
 * The judgment passes if the average score is >= 0.5.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * VotingStrategy strategy = new AverageVotingStrategy();
 * Judgment result = strategy.aggregate(judgments, Map.of());
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class AverageVotingStrategy implements VotingStrategy {

	private static final double THRESHOLD = 0.5;

	@Override
	public Judgment aggregate(List<Judgment> judgments, Map<String, Double> weights) {
		if (judgments == null || judgments.isEmpty()) {
			throw new IllegalArgumentException("Cannot aggregate empty judgment list");
		}

		double sum = judgments.stream().mapToDouble(j -> toNumerical(j.score())).sum();

		double average = sum / judgments.size();

		boolean pass = average >= THRESHOLD;

		String reasoning = String.format("Average score: %.2f (threshold: %.2f, result: %s)", average, THRESHOLD,
				pass ? "pass" : "fail");

		return Judgment.builder()
			.score(new NumericalScore(average, 0.0, 1.0))
			.status(pass ? JudgmentStatus.PASS : JudgmentStatus.FAIL)
			.reasoning(reasoning)
			.build();
	}

	@Override
	public String getName() {
		return "AverageVoting";
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
