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
import org.springaicommunity.agents.judge.Judges;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Simple jury implementation with parallel judge execution.
 *
 * <p>
 * Executes all judges in parallel using CompletableFuture and aggregates their judgments
 * using the configured VotingStrategy. Parallel execution can be disabled for sequential
 * evaluation.
 * </p>
 *
 * <p>
 * Example usage with builder:
 * </p>
 * <pre>{@code
 * Jury jury = SimpleJury.builder()
 *     .judge(fileExistsJudge, 0.3)
 *     .judge(correctnessJudge, 0.7)
 *     .votingStrategy(new WeightedAverageStrategy())
 *     .parallel(true)
 *     .build();
 *
 * Judgment result = jury.judge(context);
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class SimpleJury implements Jury {

	private final List<Judge> judges;

	private final VotingStrategy votingStrategy;

	private final Map<String, Double> weights;

	private final boolean parallel;

	private final Executor executor;

	private SimpleJury(List<Judge> judges, VotingStrategy votingStrategy, Map<String, Double> weights, boolean parallel,
			Executor executor) {
		if (judges == null || judges.isEmpty()) {
			throw new IllegalArgumentException("Jury must have at least one judge");
		}
		if (votingStrategy == null) {
			throw new IllegalArgumentException("Voting strategy is required");
		}
		this.judges = List.copyOf(judges);
		this.votingStrategy = votingStrategy;
		this.weights = Map.copyOf(weights);
		this.parallel = parallel;
		this.executor = executor != null ? executor : ForkJoinPool.commonPool();
	}

	@Override
	public List<Judge> getJudges() {
		return judges;
	}

	@Override
	public VotingStrategy getVotingStrategy() {
		return votingStrategy;
	}

	@Override
	public Verdict vote(JudgmentContext context) {
		List<Judgment> individualJudgments;

		if (parallel) {
			// Parallel execution using CompletableFuture
			List<CompletableFuture<Judgment>> futures = judges.stream()
				.map(judge -> CompletableFuture.supplyAsync(() -> judge.judge(context), executor))
				.toList();

			// Wait for all to complete
			CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

			// Collect results
			individualJudgments = allOf.thenApply(v -> futures.stream().map(CompletableFuture::join).toList()).join();
		}
		else {
			// Sequential execution
			individualJudgments = judges.stream().map(judge -> judge.judge(context)).toList();
		}

		// Build identity map (preserves order via LinkedHashMap)
		Map<String, Judgment> judgmentByName = new LinkedHashMap<>();
		for (int i = 0; i < judges.size(); i++) {
			String name = getJudgeName(judges.get(i), i);
			judgmentByName.put(name, individualJudgments.get(i));
		}

		// Aggregate using voting strategy
		Judgment aggregated = votingStrategy.aggregate(individualJudgments, weights);

		return Verdict.builder()
			.aggregated(aggregated)
			.individual(individualJudgments)
			.individualByName(judgmentByName)
			.weights(weights)
			.build();
	}

	/**
	 * Get judge name from metadata or generate default.
	 * @param judge the judge
	 * @param index the judge index
	 * @return judge name
	 */
	private String getJudgeName(Judge judge, int index) {
		return Judges.tryMetadata(judge).map(m -> m.name()).orElse("Judge#" + (index + 1));
	}

	/**
	 * Create a new builder for SimpleJury.
	 * @return builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for SimpleJury.
	 */
	public static class Builder {

		private final List<Judge> judges = new ArrayList<>();

		private final Map<String, Double> weights = new HashMap<>();

		private VotingStrategy votingStrategy;

		private boolean parallel = true;

		private Executor executor;

		/**
		 * Add a judge with equal weight (1.0).
		 * @param judge the judge to add
		 * @return this builder
		 */
		public Builder judge(Judge judge) {
			return judge(judge, 1.0);
		}

		/**
		 * Add a judge with a custom weight.
		 * @param judge the judge to add
		 * @param weight the weight for this judge
		 * @return this builder
		 */
		public Builder judge(Judge judge, double weight) {
			if (judge == null) {
				throw new IllegalArgumentException("Judge cannot be null");
			}
			if (weight < 0) {
				throw new IllegalArgumentException("Weight must be non-negative");
			}
			judges.add(judge);
			weights.put(String.valueOf(judges.size() - 1), weight);
			return this;
		}

		/**
		 * Set the voting strategy.
		 * @param votingStrategy the voting strategy
		 * @return this builder
		 */
		public Builder votingStrategy(VotingStrategy votingStrategy) {
			this.votingStrategy = votingStrategy;
			return this;
		}

		/**
		 * Enable or disable parallel execution.
		 * @param parallel true for parallel execution (default), false for sequential
		 * @return this builder
		 */
		public Builder parallel(boolean parallel) {
			this.parallel = parallel;
			return this;
		}

		/**
		 * Set custom executor for parallel execution.
		 * @param executor the executor to use
		 * @return this builder
		 */
		public Builder executor(Executor executor) {
			this.executor = executor;
			return this;
		}

		/**
		 * Build the SimpleJury instance.
		 * @return configured SimpleJury
		 */
		public SimpleJury build() {
			if (votingStrategy == null) {
				throw new IllegalStateException("Voting strategy is required");
			}
			return new SimpleJury(judges, votingStrategy, weights, parallel, executor);
		}

	}

}
