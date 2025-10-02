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
 * Consensus voting strategy requiring unanimous agreement.
 *
 * <p>
 * All judges must agree (all pass or all fail) for the verdict to pass. If any judge
 * disagrees, the result is a failing judgment. This is the strictest voting strategy.
 * </p>
 *
 * <p>
 * Boolean scores are used directly. Numerical scores are converted to boolean using a
 * threshold of 0.5.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * VotingStrategy strategy = new ConsensusStrategy();
 * Judgment result = strategy.aggregate(judgments, Map.of());
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class ConsensusStrategy implements VotingStrategy {

	private static final double THRESHOLD = 0.5;

	@Override
	public Judgment aggregate(List<Judgment> judgments, Map<String, Double> weights) {
		if (judgments == null || judgments.isEmpty()) {
			throw new IllegalArgumentException("Cannot aggregate empty judgment list");
		}

		long passCount = judgments.stream().filter(j -> toBoolean(j.score())).count();

		long failCount = judgments.size() - passCount;

		// Consensus requires all judges to agree
		boolean consensus = (passCount == judgments.size()) || (failCount == judgments.size());
		boolean pass = consensus && passCount == judgments.size();

		String reasoning;
		if (!consensus) {
			reasoning = String.format("No consensus: %d passed, %d failed (consensus required)", passCount, failCount);
		}
		else {
			reasoning = String.format("Unanimous consensus: all %d judges %s", judgments.size(),
					pass ? "passed" : "failed");
		}

		return Judgment.builder()
			.score(new BooleanScore(pass))
			.status(pass ? JudgmentStatus.PASS : JudgmentStatus.FAIL)
			.reasoning(reasoning)
			.build();
	}

	@Override
	public String getName() {
		return "Consensus";
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
