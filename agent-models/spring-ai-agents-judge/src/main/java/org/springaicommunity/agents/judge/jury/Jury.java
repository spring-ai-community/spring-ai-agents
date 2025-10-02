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
import org.springaicommunity.agents.judge.context.JudgmentContext;

import java.util.List;

/**
 * Jury of multiple judges that vote on agent execution.
 *
 * <p>
 * A Jury is a separate abstraction from Judge that aggregates judgments from multiple
 * judges using a voting strategy. Unlike Judge which returns a Judgment, Jury returns a
 * Verdict containing both the aggregated result and all individual judgments.
 * </p>
 *
 * <p>
 * The jury executes all its constituent judges (potentially in parallel) and aggregates
 * their judgments using a {@link VotingStrategy}. The final verdict includes identity
 * preservation via judge names and supports composition via sub-verdicts.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * Jury jury = SimpleJury.builder()
 *     .judge(fileExistsJudge, 0.3)
 *     .judge(correctnessJudge, 0.7)
 *     .votingStrategy(new WeightedAverageStrategy())
 *     .build();
 *
 * Verdict verdict = jury.vote(context);
 * Judgment aggregated = verdict.aggregated();
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see SimpleJury
 * @see VotingStrategy
 * @see Verdict
 */
public interface Jury {

	/**
	 * Get the list of judges in this jury.
	 * @return list of judges
	 */
	List<Judge> getJudges();

	/**
	 * Get the voting strategy used to aggregate judgments.
	 * @return voting strategy
	 */
	VotingStrategy getVotingStrategy();

	/**
	 * Execute all judges and aggregate their judgments into a verdict.
	 * @param context the judgment context
	 * @return verdict with aggregated and individual judgments
	 */
	Verdict vote(JudgmentContext context);

}
