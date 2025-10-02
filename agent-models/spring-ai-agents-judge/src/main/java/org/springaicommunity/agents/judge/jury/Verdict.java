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

import java.util.List;
import java.util.Map;

/**
 * Verdict from a jury of judges.
 *
 * <p>
 * Contains the aggregated judgment from voting strategy plus all individual judgments and
 * weights used.
 * </p>
 *
 * @param aggregated the final aggregated judgment from voting strategy
 * @param individual all individual judgments from each judge
 * @param weights the weights assigned to each judge (by judge name or index)
 * @author Mark Pollack
 * @since 0.1.0
 */
public record Verdict(Judgment aggregated, List<Judgment> individual, Map<String, Double> weights) {

	public Verdict {
		// Defensive copy for immutability
		individual = List.copyOf(individual);
		weights = Map.copyOf(weights);
	}

}
