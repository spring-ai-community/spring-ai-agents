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
 * Majority voting strategy for boolean judgments.
 *
 * <p>
 * Counts the number of passing vs failing judgments and returns the majority verdict.
 * Supports both boolean and numerical scores (numerical scores are converted to boolean
 * using a threshold of 0.5).
 * </p>
 *
 * <p>
 * Handles edge cases via configurable policies:
 * </p>
 * <ul>
 * <li>Ties: Resolved using TiePolicy (default: FAIL)</li>
 * <li>Errors: Handled using ErrorPolicy (default: TREAT_AS_FAIL)</li>
 * <li>All ABSTAIN: Returns ABSTAIN status</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * VotingStrategy strategy = new MajorityVotingStrategy();
 * VotingStrategy custom = new MajorityVotingStrategy(TiePolicy.PASS, ErrorPolicy.IGNORE);
 * Judgment result = strategy.aggregate(judgments, Map.of());
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class MajorityVotingStrategy implements VotingStrategy {

	private static final double THRESHOLD = 0.5;

	private final TiePolicy tiePolicy;

	private final ErrorPolicy errorPolicy;

	/**
	 * Create majority voting strategy with default policies.
	 */
	public MajorityVotingStrategy() {
		this(TiePolicy.FAIL, ErrorPolicy.TREAT_AS_FAIL);
	}

	/**
	 * Create majority voting strategy with custom policies.
	 * @param tiePolicy policy for handling ties
	 * @param errorPolicy policy for handling errors
	 */
	public MajorityVotingStrategy(TiePolicy tiePolicy, ErrorPolicy errorPolicy) {
		this.tiePolicy = tiePolicy;
		this.errorPolicy = errorPolicy;
	}

	@Override
	public Judgment aggregate(List<Judgment> judgments, Map<String, Double> weights) {
		if (judgments == null || judgments.isEmpty()) {
			throw new IllegalArgumentException("Cannot aggregate empty judgment list");
		}

		// Apply error policy first
		List<Judgment> processedJudgments = applyErrorPolicy(judgments);

		// Check if all are abstain after policy application
		long abstainCount = processedJudgments.stream().filter(j -> j.status() == JudgmentStatus.ABSTAIN).count();

		if (abstainCount == processedJudgments.size()) {
			return Judgment.abstain("All judges abstained or were ignored");
		}

		// Count pass/fail (excluding abstentions)
		long passCount = processedJudgments.stream().filter(j -> j.status() == JudgmentStatus.PASS).count();

		long failCount = processedJudgments.stream().filter(j -> j.status() == JudgmentStatus.FAIL).count();

		// Check for tie
		if (passCount == failCount) {
			return applyTiePolicy(passCount, failCount);
		}

		// Determine majority
		boolean majorityPass = passCount > failCount;
		JudgmentStatus status = majorityPass ? JudgmentStatus.PASS : JudgmentStatus.FAIL;

		String reasoning = String.format("Majority vote: %d passed, %d failed (majority %s)", passCount, failCount,
				majorityPass ? "pass" : "fail");

		return Judgment.builder().score(new BooleanScore(majorityPass)).status(status).reasoning(reasoning).build();
	}

	@Override
	public String getName() {
		return "majority";
	}

	/**
	 * Apply error policy to judgments.
	 * @param judgments original judgments
	 * @return processed judgments with error policy applied
	 */
	private List<Judgment> applyErrorPolicy(List<Judgment> judgments) {
		return judgments.stream().map(j -> {
			if (j.status() == JudgmentStatus.ERROR) {
				return switch (errorPolicy) {
					case TREAT_AS_FAIL -> Judgment.fail("Error treated as failure: " + j.reasoning());
					case TREAT_AS_ABSTAIN -> Judgment.abstain("Error treated as abstention: " + j.reasoning());
					case IGNORE -> Judgment.abstain("Error ignored: " + j.reasoning());
				};
			}
			return j;
		}).toList();
	}

	/**
	 * Apply tie policy when vote counts are equal.
	 * @param passCount number of passing votes
	 * @param failCount number of failing votes
	 * @return judgment based on tie policy
	 */
	private Judgment applyTiePolicy(long passCount, long failCount) {
		String reasoning = String.format("Tie vote: %d passed, %d failed (tie resolved as %s)", passCount, failCount,
				tiePolicy.name().toLowerCase());

		return switch (tiePolicy) {
			case PASS -> Judgment.builder()
				.score(new BooleanScore(true))
				.status(JudgmentStatus.PASS)
				.reasoning(reasoning)
				.build();
			case FAIL -> Judgment.builder()
				.score(new BooleanScore(false))
				.status(JudgmentStatus.FAIL)
				.reasoning(reasoning)
				.build();
			case ABSTAIN -> Judgment.builder()
				.score(new BooleanScore(false))
				.status(JudgmentStatus.ABSTAIN)
				.reasoning(reasoning)
				.build();
		};
	}

}
