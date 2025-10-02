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
 * A Jury is itself a Judge (composable pattern), allowing juries to be used anywhere a
 * single judge can be used. This enables recursive composition and flexible evaluation
 * strategies.
 * </p>
 *
 * <p>
 * The jury executes all its constituent judges (potentially in parallel) and aggregates
 * their judgments using a {@link VotingStrategy}. The final verdict includes both the
 * aggregated judgment and all individual judgments.
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
 * Judgment aggregated = jury.judge(context);
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see SimpleJury
 * @see VotingStrategy
 * @see Verdict
 */
public interface Jury extends Judge {

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
