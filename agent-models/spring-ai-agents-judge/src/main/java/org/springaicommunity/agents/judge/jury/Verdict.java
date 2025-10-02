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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verdict from a jury of judges.
 *
 * <p>
 * Contains the aggregated judgment from voting strategy plus all individual judgments
 * with identity preservation and optional sub-verdicts for meta-jury composition.
 * </p>
 *
 * @param aggregated the final aggregated judgment from voting strategy
 * @param individual all individual judgments from each judge (ordered)
 * @param individualByName judgments indexed by judge name for identity preservation
 * @param weights the weights assigned to each judge (by judge name or index)
 * @param subVerdicts nested verdicts from sub-juries (for MetaJury composition)
 * @author Mark Pollack
 * @since 0.1.0
 */
public record Verdict(Judgment aggregated, List<Judgment> individual, Map<String, Judgment> individualByName,
		Map<String, Double> weights, List<Verdict> subVerdicts) {

	public Verdict {
		// Defensive copy for immutability
		individual = individual != null ? List.copyOf(individual) : List.of();
		individualByName = individualByName != null ? Map.copyOf(individualByName) : Map.of();
		weights = weights != null ? Map.copyOf(weights) : Map.of();
		subVerdicts = subVerdicts != null ? List.copyOf(subVerdicts) : List.of();
	}

	/**
	 * Create a builder for Verdict.
	 * @return new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for Verdict.
	 */
	public static class Builder {

		private Judgment aggregated;

		private List<Judgment> individual = new ArrayList<>();

		private Map<String, Judgment> individualByName = new HashMap<>();

		private Map<String, Double> weights = new HashMap<>();

		private List<Verdict> subVerdicts = new ArrayList<>();

		public Builder aggregated(Judgment aggregated) {
			this.aggregated = aggregated;
			return this;
		}

		public Builder individual(List<Judgment> individual) {
			this.individual = new ArrayList<>(individual);
			return this;
		}

		public Builder individualByName(Map<String, Judgment> individualByName) {
			this.individualByName = new HashMap<>(individualByName);
			return this;
		}

		public Builder weights(Map<String, Double> weights) {
			this.weights = new HashMap<>(weights);
			return this;
		}

		public Builder subVerdicts(List<Verdict> subVerdicts) {
			this.subVerdicts = new ArrayList<>(subVerdicts);
			return this;
		}

		public Verdict build() {
			return new Verdict(aggregated, individual, individualByName, weights, subVerdicts);
		}

	}

}
