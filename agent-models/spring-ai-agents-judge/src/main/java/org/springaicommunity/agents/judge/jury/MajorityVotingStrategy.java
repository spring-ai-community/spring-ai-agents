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
 * Majority voting strategy for boolean judgments.
 *
 * <p>
 * Counts the number of passing vs failing judgments and returns the majority verdict.
 * Supports both boolean and numerical scores (numerical scores are converted to boolean
 * using a threshold of 0.5).
 * </p>
 *
 * <p>
 * In case of a tie, the result is a failing judgment.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * VotingStrategy strategy = new MajorityVotingStrategy();
 * Judgment result = strategy.aggregate(judgments, Map.of());
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class MajorityVotingStrategy implements VotingStrategy {

	private static final double THRESHOLD = 0.5;

	@Override
	public Judgment aggregate(List<Judgment> judgments, Map<String, Double> weights) {
		if (judgments == null || judgments.isEmpty()) {
			throw new IllegalArgumentException("Cannot aggregate empty judgment list");
		}

		long passCount = judgments.stream().filter(j -> toBoolean(j.score())).count();

		long failCount = judgments.size() - passCount;

		boolean majorityPass = passCount > failCount;

		String reasoning = String.format("Majority vote: %d passed, %d failed (majority %s)", passCount, failCount,
				majorityPass ? "pass" : "fail");

		return Judgment.builder().score(new BooleanScore(majorityPass)).pass(majorityPass).reasoning(reasoning).build();
	}

	@Override
	public String getName() {
		return "MajorityVoting";
	}

	/**
	 * Convert any score type to boolean.
	 * @param score the score to convert
	 * @return true if score represents a pass
	 */
	private boolean toBoolean(Score score) {
		if (score instanceof BooleanScore bs) {
			return bs.value();
		}
		else if (score instanceof NumericalScore ns) {
			return ns.normalized() >= THRESHOLD;
		}
		return false;
	}

}
