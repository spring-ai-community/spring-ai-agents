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
import org.springaicommunity.agents.judge.result.Judgment;

import java.util.List;
import java.util.Map;

/**
 * Meta-jury that aggregates verdicts from multiple sub-juries.
 *
 * <p>
 * Package-private implementation used by {@link Juries} utility for jury-of-juries
 * composition. Executes multiple juries and aggregates their verdicts using a voting
 * strategy.
 * </p>
 *
 * <p>
 * The meta-jury treats each jury's aggregated judgment as an individual judgment for
 * voting purposes, and preserves all sub-verdicts in the final verdict for full
 * traceability.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class MetaJury implements Jury {

	private final List<Jury> juries;

	private final VotingStrategy metaStrategy;

	/**
	 * Create a meta-jury from sub-juries.
	 * @param juries the sub-juries to aggregate
	 * @param metaStrategy the voting strategy for aggregating jury verdicts
	 */
	MetaJury(List<Jury> juries, VotingStrategy metaStrategy) {
		if (juries == null || juries.isEmpty()) {
			throw new IllegalArgumentException("At least one jury is required");
		}
		if (metaStrategy == null) {
			throw new IllegalArgumentException("Meta voting strategy is required");
		}
		this.juries = List.copyOf(juries);
		this.metaStrategy = metaStrategy;
	}

	@Override
	public List<Judge> getJudges() {
		// Meta-jury doesn't have direct judges, only sub-juries
		return List.of();
	}

	@Override
	public VotingStrategy getVotingStrategy() {
		return metaStrategy;
	}

	@Override
	public Verdict vote(JudgmentContext context) {
		// Execute all sub-juries
		List<Verdict> subVerdicts = juries.stream().map(jury -> jury.vote(context)).toList();

		// Extract aggregated judgments from each jury verdict
		List<Judgment> aggregatedJudgments = subVerdicts.stream().map(Verdict::aggregated).toList();

		// Aggregate the jury verdicts using meta strategy
		Judgment metaJudgment = metaStrategy.aggregate(aggregatedJudgments, Map.of());

		// Build final verdict with sub-verdicts preserved
		return Verdict.builder()
			.aggregated(metaJudgment)
			.individual(aggregatedJudgments)
			.subVerdicts(subVerdicts)
			.build();
	}

}
